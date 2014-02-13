/*
Copyright 2014 Google Inc. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

var readgraph = new function() {
  var cigarMatcher = /([0-9]+[MIDNSHP=X])/gi

  var width = 0;
  var height = 800;
  var margin = 30;

  var textHeight, textWidth = 0;

  var y = d3.scale.linear().range([margin, height - margin*2]);
  var x = d3.scale.linear();
  var xAxis = d3.svg.axis().ticks(5).scale(x);
  var xFormat = d3.format(',f');

  var zoom = null;
  var maxZoom = 1;
  var zoomLevelChange = 1;
  var minRange = 0;

  var opacity = d3.scale.linear().domain([0, 93]).range([.2, 1]);
  var radiusScale = d3.scale.linear().range([0, 2 * Math.PI]);
  var summaryYScale = d3.scale.linear().domain([0, 1]).range([height *.4, height *.5]);
  var coverageYScale = d3.scale.linear().domain([40, 0]).range([margin*2, height - margin*2]).clamp(true);

  // Current state
  var readsetIds = [];
  var targets = null;
  var currentTarget = null;
  var xhrTimeout = null;

  // Dom elements
  var svg, readDiv, spinner = null;

  var getScaleLevel = function() {
    return Math.floor(Math.log(zoom.scale()) / Math.log(zoomLevelChange) + .1);
  };

  var handleZoom = function() {
    var tx = zoom.translate()[0];
    tx = Math.max(tx, (1 - zoom.scale()) * width); // TODO: This isn't strict enough
    tx = Math.min(tx, 0);
    zoom.translate([tx, 0]);
    svg.select(".axis").call(xAxis);

    // Update scale bar
    d3.select('.zoomLevel').attr('y', (6 - getScaleLevel()) * 24 + 38);
    updateDisplay();
  };

  var moveToTargetPosition = function(position) {
    position = Math.max(0, position);
    position = Math.min(currentTarget.targetLength, position);

    var newX = x(position);
    newX = zoom.translate()[0] - newX + width / 2;
    zoom.translate([newX, 0]);
    handleZoom();
  };

  var setupRun = false;
  var setup = function() {
    setupRun = true;

    // Measurements
    svg = d3.select("#graph");
    var text = addText('G', 0, 0);
    var bbox = text.node().getBBox();
    textWidth = bbox.width;
    textHeight = bbox.height;
    text.remove();

    width = $('#graph').width();
    x.rangeRound([margin, width - margin]);
    minRange = (width / textWidth / 2); // Twice the zoom of individual bases

    readDiv = $('#readDiv');


    // Svg init
    // Reads Axis
    svg.append('g')
        .attr('transform', 'translate(0,' + (height - margin) + ')')
        .attr('class', 'axis');

    // Target coverage
    svg.append('path').attr('class', 'coverageSummary');
    svg.append('path').attr('class', 'coverage');

    // Hover line
    var hoverline = svg.append("line")
        .attr("class", "hover hoverline")
        .attr("x1", 0).attr("x2", 0)
        .attr("y1", 0).attr("y2", height);

    var hovertext = svg.append('text')
        .attr("class", "hover hovertext")
        .attr('y', textHeight);

    svg.on("mousemove", function() {
      var mouseX = d3.mouse(this)[0];
      mouseX = Math.max(margin, mouseX);
      mouseX = Math.min(width - margin, mouseX);

      if (mouseX > width * 2/3) {
        hovertext.attr('x', mouseX - 3).style('text-anchor', 'end');
      } else {
        hovertext.attr('x', mouseX + 3).style('text-anchor', 'start');
      }
      hovertext.text(xFormat(x.invert(mouseX)));
      hoverline.attr("x1", mouseX).attr("x2", mouseX)
    });

    // Zooming
    var changeZoomLevel = function(levelChange) {
      var newZoom = zoom.scale();
      var middleX = x.invert(width / 2); // Keep the graph centered on the middle position

      if (levelChange > 0) {
        newZoom = zoom.scale() * zoomLevelChange;
      } else {
        newZoom = zoom.scale() / zoomLevelChange;
      }
      newZoom = Math.max(1, newZoom);
      newZoom = Math.min(maxZoom, newZoom);
      zoom.scale(newZoom);
      console.log("switched to zoom: " + newZoom + " and scale " + getScaleLevel());

      handleZoom();
      moveToTargetPosition(middleX);
    };

    zoom = d3.behavior.zoom().size([width, height]).on("zoom", handleZoom);
    svg.call(zoom);

    addImage('zoom-bar.png', 10, 201, 7, 10);
    addImage('zoom-level.png', 22, 15, 2, 183, null, 'zoomLevel');
    addImage('zoom-plus.png', 25, 25, 0, 10, function() {
      changeZoomLevel(1);
    });
    addImage('zoom-minus.png', 25, 25, 0, 200, function() {
      changeZoomLevel(-1);
    });
    var zoomTextX = 23;
    addText('Bases', zoomTextX, 50);
    addText('Reads', zoomTextX, 98);
    addText('Coverage', zoomTextX, 147);
    addText('Summary', zoomTextX, 195);

    // Spinner
    spinner = addImage('spinner.gif', 16, 16, width - 16, 0);
    spinner.style('display', 'none');
  };

  this.jumpGraph = function(position) {
    // TODO: Support non-int positions - feature, gene, etc
    position = parseInt(position.replace(/,/g, ''));
    if (position != 0 && !position) {
      showMessage('Only numbered positions are supported right now');
      return;
    }

    var zoomLevel = maxZoom / (zoomLevelChange * 2); // Read level
    if (zoom.scale() != zoomLevel) {
      zoom.scale(zoomLevel);
      handleZoom();
    }
    moveToTargetPosition(position);
  };

  var addImage = function(name, width, height, x, y, opt_handler, opt_class) {
    return svg.append('image').attr('xlink:href', '/static/img/' + name)
        .attr('width', width).attr('height', height)
        .attr('x', x).attr('y', y)
        .on("mouseup", opt_handler || function(){})
        .attr('class', opt_class || '');
  };

  var addText = function(name, x, y) {
    return svg.append('text').text(name).attr('x', x).attr('y', y);
  };

  var selectTarget = function(target) {
    currentTarget = target;
    $('.target').removeClass('active');
    $('#target-' + target.name).addClass('active');
    $('#graph').show();
    $('#circleGraph').hide();
    if (!setupRun) {
      setup();
    }

    // Axis and zoom
    x.domain([0, target.targetLength]);
    maxZoom = Math.ceil(Math.max(1, target.targetLength / minRange));
    zoomLevelChange = Math.pow(maxZoom, 1/6);
    zoom.x(x).scaleExtent([1, maxZoom]).size([width, height]);

    // Set summary data
    svg.select('.coverageSummary').datum(getFakeSummary(target));

    $('#jumpDiv').show();
    handleZoom();
  };

  var updateTargets = function() {
    var targetsDiv = $("#targets").empty();

    var totalTargetLength = 0;
    $.each(targets, function(i, target) {
      var targetDiv = $('<div/>', {'class': 'target', id: 'target-' + target.name}).appendTo(targetsDiv);
      $('<img>', {'class': 'pull-left', src: '/static/img/chr' + target.name + '.png'}).appendTo(targetDiv);
      $('<div>', {'class': 'title'}).text("Chromosome " + target.name).appendTo(targetDiv);
      $('<div>', {'class': 'summary'}).text(xFormat(target.targetLength) + " bases").appendTo(targetDiv);

      targetDiv.click(function() {
        selectTarget(target);
      });

      // Circle stats
      target.radiusStart = totalTargetLength;
      totalTargetLength += target.targetLength;
      target.radiusEnd = totalTargetLength;
    });
  };

  var getFakeSummary = function(target) {
    var points = [];
    for (var i = 0; i < target.targetLength / 1000000; i++) {
      points.push({'sx': i * 1000000, 'sy': Math.random() });
    }
    return points;
  };

  var updateDisplay = function() {
    var scaleLevel = getScaleLevel();
    var summaryView = scaleLevel < 2;
    var coverageView = scaleLevel == 2 || scaleLevel == 3;
    var readView = scaleLevel > 3;
    var baseView = scaleLevel > 5;

    var summary = svg.selectAll(".coverageSummary");
    var coverage = svg.selectAll(".coverage");
    var reads = svg.selectAll(".read");
    toggleVisibility(summary, summaryView);
    toggleVisibility(coverage, coverageView);
    toggleVisibility(reads, readView);

    var targetStart = parseInt(x.domain()[0]);
    var targetEnd = parseInt(x.domain()[1]);

    if (summaryView) {
      summary.attr('d', d3.svg.line()
              .x(function(d) { return x(d.sx); })
              .y(function(d) { return summaryYScale(d.sy); }));

    } else if (coverageView) {
      queryCoverage(targetStart, targetEnd);
      if (coverage.datum()) {
        coverage.attr('d', d3.svg.line()
            .x(function(d) { return x(d.cx); })
            .y(function(d) { return coverageYScale(d.cy); }));
      }

    } else if (readView) {
      queryReads(targetStart, targetEnd);

      reads.selectAll('.outline')
          .attr("points", outlinePoints);

      if (!baseView) {
        reads.selectAll('.letter').style('display', 'none');
      } else {
        reads.selectAll('.letter')
            .style('display', function(data, i) {
              if (data.rx < targetStart || data.rx >= targetEnd - 1) {
                return 'none';
              } else {
                return 'block';
              }
            })
            .attr("x", function(data, i) {
              return x(data.rx) + textWidth;
            })
            .attr("y", function(data, i) {
              return y(data.ry) + textHeight;
            });
      }
    }
  };

  var toggleVisibility = function(items, visible) {
    items.style('display', visible ? 'block' : 'none');
  };

  // Read position
  var stringifyPoints = function(points) {
    for (var i = 0; i < points.length; i++) {
      points[i] = points[i].join(',');
    }
    return points.join(' ');
  };

  var outlinePoints = function(read, i) {
    var yTracksLength = y.domain()[0];
    var barHeight = Math.min(30, Math.max(2, (height - margin*3)/ yTracksLength - 5));

    var pointWidth = 10;
    var startX = Math.max(margin, x(read.position));
    var endX = Math.min(width - margin, x(read.end));

    if (startX > endX - pointWidth) {
      return '0,0';
    }

    var startY = y(read.yOrder);
    var endY = startY + barHeight;
    var midY = (startY + barHeight / 2);


    if (read.reverse) {
      startX += pointWidth;
    } else {
      endX -= pointWidth;
    }

    var points = [];
    points.push([startX, startY]);
    if (read.reverse) {
      points.push([startX - pointWidth, midY]);
    }
    points.push([startX, endY]);
    points.push([endX, endY]);
    if (!read.reverse) {
      points.push([endX + pointWidth, midY]);
    }
    points.push([endX, startY]);
    return stringifyPoints(points);
  };

  // Read details
  var showRead = function(read, i) {
    readDiv.empty().show();
    $("<h4/>").text("Read: " + read.qname).appendTo(readDiv);
    var dl = $("<dl/>").addClass("dl").appendTo(readDiv);
    $("<dt/>").text("Position").appendTo(dl);
    $("<dd/>").text(read.position).appendTo(dl);
    $("<dt/>").text("Length").appendTo(dl);
    $("<dd/>").text(read.length).appendTo(dl);
    $("<dt/>").text("Mapping quality").appendTo(dl);
    $("<dd/>").text(read.mappingQuality).appendTo(dl);
    $("<dt/>").text("Cigar").appendTo(dl);
    $("<dd/>").text(read.cigar).appendTo(dl);
//    $("<dt/>").text("Y Order").appendTo(dl);
//    $("<dd/>").text(read.yOrder).appendTo(dl);
//    $("<dt/>").text("End").appendTo(dl);
//    $("<dd/>").text(read.end).appendTo(dl);
//    $("<dt/>").text("Index").appendTo(dl);
//    $("<dd/>").text(read.index).appendTo(dl);
    d3.select(this).classed("selected", true);
  };

  var hideRead = function(read, i) {
    d3.select(this).classed("selected", false);
  };

  var setCoverage = function(targetStart, targetEnd, reads) {
    var coverage = [];
    $.each(reads, function(i, read) {
      // TODO: Get this from the api rather than computing ourselves
      var start = read.position - targetStart;
      var end = start + read.alignedSequence.length;
      for (i = start; i < end; i++) {
        coverage[i] = (coverage[i] || 0) + 1;
      }
    });

    for (var i = 0; i < coverage.length; i++) {
      coverage[i] =  {cx: targetStart + i, cy: coverage[i] || 0};
    }
    svg.select('.coverage').datum(coverage);
    updateDisplay();
  };

  var setYOrder = function(read, yOrder) {
    read.yOrder = yOrder;

    for (var r = 0; r < read.readPieces.length; r++) {
      read.readPieces[r].ry = read.yOrder;
    }
  };

  var setReads = function(reads) {
    // TODO: Pair up the reads somehow... Possibly move some logic to python
    var yTracks = [];
    $.each(reads, function(readi, read) {
      // Interpret the cigar
      // TODO: Should this even use the cigar or just use a reference thing??
      var bases = read.originalSequence.split('');
      var matches = read.cigar.match(cigarMatcher);
      var baseIndex = 0;

      read.id = read.qname + read.position + read.cigar;
      read.readPieces = [];
      if (!read.cigar) {
        // Hack for unmapped reads
        read.length = 0;
        read.end = read.position;
        return;
      }

      for (var m = 0; m < matches.length; m++) {
        var match = matches[m];
        var baseCount = parseInt(match);
        var baseType = match.match(/[^0-9]/)[0];

        switch (baseType) {
          case 'H':
          case 'P':
            // We don't display clipped sequences right now
            continue;
          case 'D':
          case 'N':
            // Deletions get placeholders inserted
            for (var b = 0; b < baseCount; b++) {
              read.readPieces.push({
                'letter' : '-',
                'rx': read.position + read.readPieces.length,
                'qual': 100 // TODO: What is the qual here??
              });
            }
            continue;
          case 'I': // TODO: What should an insertion look like?
          case 'x': // TODO: Color these differently?
          case 'M':
          case '=':
          case 'S': // TODO: Are we suppose to hide clipped bases from the ui?
            for (var j = 0; j < baseCount; j++) {
              read.readPieces.push({
                'letter' : bases[baseIndex],
                'rx': read.position + read.readPieces.length,
                'qual': read.qual.charCodeAt(baseIndex) - 33
              });
              baseIndex++;
            }
            continue;
        }
      }

      read.length = read.readPieces.length;
      read.end = read.position + read.length;
      read.reverse = (read.flags >> 4) % 2 == 1; // The 5th flag bit indicates this read is reversed
      read.index = readi;

      for (var i = 0; i < yTracks.length; i++) {
        if (yTracks[i] < read.position) {
          yTracks[i] = read.end;
          setYOrder(read, i);
          return
        }
      }

      setYOrder(read, yTracks.length);
      yTracks.push(read.end);
    });

    y.domain([yTracks.length, -1]);

    // Update the data behind the graph
    // TODO: Extract out so that the data can be refreshed all the time
    // (ie we should do our own filtering based on the target start/end)

    svg.selectAll('.read').remove();
    if (reads.length == 0) {
      return;
    }

    var reads = svg.selectAll(".read").data(reads, function(read){return read.id;});

    reads.enter().append("g")
        .attr('class', 'read')
        .on("mouseover", showRead)
        .on("mouseout", hideRead);

    var outlines = reads.selectAll('.outline')
        .data(function(read, i) { return [read];});
    outlines.enter().append('polygon')
        .attr('class', 'outline');

    var letters = reads.selectAll(".letter")
        .data(function(read, i) { return read.readPieces; });

    letters.enter().append('text')
        .attr('class', 'letter')
        .style('opacity', function(data, i) { return opacity(data.qual); })
        .text(function(data, i) { return data.letter; });

    reads.exit().remove();
    updateDisplay();
  };

  var makeQueryParams = function(targetStart, targetEnd, type) {
    var queryParams = {};
    queryParams.readsetIds = readsetIds.join(',');
    queryParams.type = type;
    queryParams.target = currentTarget.name;
    queryParams.targetStart = parseInt(targetStart);
    queryParams.targetEnd = parseInt(targetEnd);
    return queryParams;
  };

  var queryReads = function(targetStart, targetEnd) {
    queryApi(targetStart, targetEnd, 'reads', setReads);
  };

  var queryCoverage = function(targetStart, targetEnd) {
    queryApi(targetStart, targetEnd, 'coverage', function(reads) {
      setCoverage(targetStart, targetEnd, reads);
    });
  };

  var lastQueryParams = null;
  var queryApi = function(targetStart, targetEnd, type, handler) { // TODO: Make this cleaner
    var queryParams = makeQueryParams(targetStart, targetEnd, type);

    if (lastQueryParams
        && lastQueryParams.readsetIds == queryParams.readsetIds
        && lastQueryParams.type == queryParams.type
        && lastQueryParams.target == queryParams.target
        && lastQueryParams.targetStart <= queryParams.targetStart
        && lastQueryParams.targetEnd >= queryParams.targetEnd) {
      return;
    }

    if (xhrTimeout) {
      clearTimeout(xhrTimeout);
    }

    xhrTimeout = setTimeout(function() {
      callXhr('/api/reads', queryParams, handler);
    }, 500);
  };

  var callXhr = function(url, queryParams, handler, opt_reads) {
    spinner.style('display', 'block');
    $.getJSON(url, queryParams)
        .done(function(res) {
          lastQueryParams = queryParams;
          var reads = (res.reads || []).concat(opt_reads || []);
          handler(reads);

          if (res.nextPageToken) {
            console.log("Current total reads is " + reads.length + " next page token is: " + res.nextPageToken);
            queryParams['pageToken'] = res.nextPageToken;
            callXhr(url, queryParams, handler, reads);
          }
        })
        .fail(function(xhr) {
          showError("Sorry, the api request failed for some reason. Better error handling to come! (" + xhr.responseText + ")");
        })
        .always(function() {
          spinner.style('display', 'none');
        });
  };

  this.hasReadset = function(id) {
    return $.inArray(id, readsetIds) != -1;
  };

  this.addReadset = function(id, targetData) {
    // TODO: Support multiple readsets
    readsetIds = [id];
    if (readsetIds.length == 1) {
      targets = targetData;
      updateTargets();
      $('#chooseReadsetMessage').hide();
    } else {
      // TODO: Refresh the graph data
      // Eventually, targets will actually be different for different readsets, so this should update the list
    }
  };

  this.removeReadset = function(id) {
    readsetIds = _.without(readsetIds, id);
    if (readsetIds.length == 0) {
      targets = [];
      updateTargets();
      $('#chooseReadsetMessage').show();
      $('#graph').hide();
      $('#jumpDiv').hide();
    } else {
      // TODO: Update target list and graph data
    }
  }
};
