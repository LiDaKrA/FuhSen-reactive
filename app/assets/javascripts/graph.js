
var d3Graph = {};
var width = 960,
    height = 650,r=70;

var rectW = 2*r;
var rectH = r;

var typeToColor = {
    "PERSON" : "#ffcc66",
    "LOC" : "#99ddff",
    "ORG" : "#85e085",
    "LITERAL" : "#fff"};

d3Graph.force = null;
d3Graph.svg = null;
d3Graph.create = function(el, props, graph) {

   this.force = d3.layout.force()
        .charge(props.charge)
        .linkDistance(props.linkDist)
        .linkStrength(props.linkStrength)
        .gravity(props.gravity)
        .size([props.width, props.height]);


   this.svg = d3.select(el).append('svg')
        .attr('width', props.width)
        .attr('height', props.height);

    // define arrow markers for graph links
    this.svg.append('svg:defs').append('svg:marker')
        .attr('id', 'end-arrow')
        .attr('viewBox', '0 -5 10 10')
        .attr('refX', 6)
        .attr('markerWidth', 8)
        .attr('markerHeight', 8)
        .attr('orient', 'auto')
        .append('svg:path')
        .attr('d', 'M0,-5L10,0L0,5')
        .attr('fill', 'red');

    this.svg.append('svg:defs').append('svg:marker')
        .attr('id', 'start-arrow')
        .attr('viewBox', '0 -5 10 10')
        .attr('refX', 4)
        .attr('markerWidth', 10)
        .attr('markerHeight', 10)
        .attr('orient', 'auto')
        .append('svg:path')
        .attr('d', 'M10,-5L0,0L10,5')
        .attr('fill', 'red');


    this.update(graph);
    this.force.nodes(graph.nodes)
        .links(graph.links).start();
};

d3Graph.update = function (graph) {
    // path (link) group
    var path = this.svg.append('svg:g').selectAll('path')
        .data(graph.links)
        .enter().append('svg:path')
        .attr('class', 'link')
        .attr("id",function(d,i) {return 'edge'+i;})
        .style('marker-end', 'url(#end-arrow)');


    var edgelabels = this.svg.selectAll(".edgelabel")
        .data(graph.links)
        .enter()
        .append('text')
        .style("pointer-events", "none")
        .attr({'class':'edgelabel',
            'id':function(d,i){return 'edgelabel'+i;},
            'dx':80,
            'dy':0,
            'font-size':10,
            'fill':'#aaa'});

    edgelabels.append('textPath')
        .attr('xlink:href',function(d,i) {return '#edge'+i;})
        .text(function(d,i){return d.label;});

    var nodeContainer = this.svg.append('svg:g').selectAll(".node")
        .data(graph.nodes)
        .enter().append('svg:g');

    nodeContainer.filter(function(d){ return d.type ==  "LITERAL"; })
        .append('svg:rect')
        .attr("width", rectW)
        .attr("height", rectH)
        .attr("class", "node")
        .style("fill", function(d) { return typeToColor[d.type]; })
        .style("stroke-dasharray","5,5")
        .call(this.force.drag);

    nodeContainer
        .filter(function(d){ return d.type !=  "LITERAL"; })
        .append('svg:circle')
        .attr("class", "node")
        .attr("r", r)
        .style("fill", function(d) { return typeToColor[d.type]; })
        .call(this.force.drag);


    nodeContainer.filter(function(d){ return d.type !=  "LITERAL"; })
        .append("svg:text")
        .text(function (d) {
            return d.name;
        })//.style("font-size", function(d) { return Math.min(2 * r, (2 * r - 8) / this.getComputedTextLength() * 20) + "px"; })
        .attr("dy", ".08em")
        .call(wrap, 2*r);

    nodeContainer.filter(function(d){ return d.type ==  "LITERAL"; })
        .append("svg:text")
        .style("alignment-baseline","central")
        .attr("x",rectW/2)
        .attr("y",rectH/2)
        .text(function (d) {
            return d.name;
        }).style("font-size", function(d) { return Math.min(rectW, (rectW - 8) / this.getComputedTextLength() * 20) + "px"; });

    this.force.on("tick", function() {
        d3Graph._drawGraph(path,nodeContainer,edgelabels);
    });

};
d3Graph._drawGraph = function(path,nodeContainer,edgelabels){

    path.attr('d', function(d) {
        var startX = d.source.x,startY = d.source.y,endX= d.target.x,endY = d.target.y;
        if(d.source.type == "LITERAL"){
            startX += rectW/2;
            startY += rectH/2;
        }
        if(d.target.type == "LITERAL"){
            endX += rectW/2;
            endY += rectH/2;
        }
        var diagonalDist = Math.sqrt(rectW/2 * rectW/2 + rectH/2 * rectH/2);
        var deltaX = endX - startX,
            deltaY = endY - startY,
            dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY),
            normX = deltaX / dist,
            normY = deltaY / dist,
            sourcePadding = (d.source.type != "LITERAL" ? r : diagonalDist),
            targetPadding = (d.target.type != "LITERAL" ? r + 5 : diagonalDist),
            sourceX = startX + (sourcePadding * normX),
            sourceY = startY + (sourcePadding * normY),
            targetX = endX - (targetPadding * normX),
            targetY = endY - (targetPadding * normY);

        return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
    });

    nodeContainer.attr('transform', function(d) {
        return 'translate(' + d.x + ',' + d.y + ')';
    });

    edgelabels.attr('transform',function(d,i){
        if (d.target.x<d.source.x){
            bbox = this.getBBox();
            rx = bbox.x+bbox.width/2;
            ry = bbox.y+bbox.height/2;
            return 'rotate(180 '+rx+' '+ry+')';
        }
        else {
            return 'rotate(0)';
        }
    });

};
d3Graph.destroy = function() {
    d3.select("svg").remove();
    this.force = null;
    this.svg = null;
};

function wrap(text, width) {
    text.each(function() {
        var text = d3.select(this),
            words = text.text().split(/\s+/).reverse(),
            word,
            line = [],
            lineNumber = 0,
            lineHeight = 1.0, // ems
            y = text.attr("y"),
            dy = parseFloat(text.attr("dy")),
            tspan = text.text(null).append("tspan").attr("x", 0).attr("y", y).attr("dy", dy + "em");
        while (!!(word = words.pop())) {
            line.push(word);
            tspan.text(line.join(" "));
            if (tspan.node().getComputedTextLength() > width) {
                line.pop();
                tspan.text(line.join(" "));
                line = [word];
                tspan = text.append("tspan").attr("x", 0).attr("y", y).attr("dy", lineHeight + dy + "em").text(word);
            }
        }
    });
}
