var RichText = React.createClass({
    OnClickMoreDetails: function (){
        alert(this.props.label + ":\n" + this.props.text);
        return false;
    },
    render: function () {
        var text = this.props.text;
        var moredata = false;
        var maxLenght = parseInt(this.props.maxLength);
        if($.trim(text).length > maxLenght){
            text =  text.substring(0,maxLenght);
            moredata = true;
        }
        return (
                <span>
                    {text}
                    {moredata ? <a href="#" onClick={this.OnClickMoreDetails}> ...read details...</a> : null}
                </span>
        );
    }
});

var Graph = React.createClass({
    convertData : function (entity) {
        if(entity["@type"] === "foaf:Person")
            return this.convertPersonData(entity);
        else if (entity["@type"] === "foaf:Organization")
            return this.convertOrgData(entity);
        else if (entity["@type"] === "gr:ProductOrService")
            return this.convertProductData(entity);
    },
    convertPersonData : function(entity){
        alert("convertPersonData");
        var graph = {"nodes" : [],"links": []};

        //Generate Graph
        graph["nodes"].push({"name":entity["fs:title"],"type":"PERSON"});

        if (entity.image !== undefined) {
            graph["nodes"].push({"name": entity.image, "type": "IMAGE"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":"image"});
        }

        if (entity["fs:location"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:location"], "type": "LOC"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("location")});
        }
        if (entity["fs:alias"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:alias"], "type": "LITERAL"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("nick")});
        }

        if (entity["fs:country"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:country"], "type": "LOC"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("country")});
        }

        if (entity["fs:gender"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:gender"], "type": "LITERAL"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("gender")});
        }

        graph["nodes"].push({"name":entity["fs:source"],"type":"LITERAL"});
        graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("source")});

        if (entity["fs:label"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:label"], "type": "LITERAL"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":"Label"});
        }

        if (entity["fs:birthday"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:birthday"], "type": "LITERAL"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("birthday")});
        }

        if (entity["fs:occupation"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:occupation"], "type": "LITERAL"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("occupation")});
        }

        if (entity["fs:liveIn"] !== undefined) {
            if(Array.isArray(entity["fs:liveIn"])) {
                for (var i = 0; i < entity["fs:liveIn"].length; i++) {
                    graph["nodes"].push({"name": entity["fs:liveIn"][i], "type": "LOC"});
                    graph["links"].push({
                        "source": 0,
                        "target": graph["nodes"].length - 1,
                        "label": getTranslation("liveIn")
                    });
                }
            }
            else{
                graph["nodes"].push({"name": entity["fs:liveIn"], "type": "LOC"});
                graph["links"].push({
                    "source": 0,
                    "target": graph["nodes"].length - 1,
                    "label": getTranslation("liveIn")
                });
            }
        }
        if (entity["fs:workAt"] !== undefined) {
            if(Array.isArray(entity["fs:workAt"])){
                for (var i = 0; i < entity["fs:workAt"].length; i++) {
                    graph["nodes"].push({"name": entity["fs:workAt"][i], "type": "ORG"});
                    graph["links"].push({
                        "source": 0,
                        "target": graph["nodes"].length - 1,
                        "label": getTranslation("workAt")
                    });
                }
            }
            else{
                graph["nodes"].push({"name": entity["fs:workAt"], "type": "ORG"});
                graph["links"].push({
                    "source": 0,
                    "target": graph["nodes"].length - 1,
                    "label": getTranslation("workAt")
                });
            }
        }
        if (entity["fs:studyAt"] !== undefined) {
            if(Array.isArray(entity["fs:studyAt"])){
                for (var i = 0; i < entity["fs:studyAt"].length; i++) {
                    graph["nodes"].push({"name": entity["fs:studyAt"][i], "type": "ORG"});
                    graph["links"].push({
                        "source": 0,
                        "target": graph["nodes"].length - 1,
                        "label": getTranslation("studyAt")
                    });
                }
            }
            else{
                graph["nodes"].push({"name": entity["fs:studyAt"], "type": "ORG"});
                graph["links"].push({
                    "source": 0,
                    "target": graph["nodes"].length - 1,
                    "label": getTranslation("studyAt")
                });
            }
        }
        return graph;
    },
    convertOrgData : function(entity){
        var graph = {"nodes" : [],"links": []};

        //Generate Graph
        graph["nodes"].push({"name":entity["fs:title"],"type":"ORG"});

        if (entity.image !== undefined) {
            graph["nodes"].push({"name": entity.image, "type": "IMAGE"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":"image"});
        }

        if (entity["fs:location"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:location"], "type": "LOC"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("location")});
        }

        if (entity["fs:country"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:country"], "type": "LOC"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("country")});
        }
        graph["nodes"].push({"name":entity["fs:source"],"type":"LITERAL"});
        graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("source")});

        if (entity["fs:label"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:label"], "type": "LITERAL"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":"Label"});
        }
        return graph;
    },
    convertProductData: function (entity) {

        var graph = {"nodes" : [],"links": []};

        //Generate Graph
        graph["nodes"].push({"name":entity["fs:title"],"type":"ORG"});

        if (entity.image !== undefined) {
            graph["nodes"].push({"name": entity.image, "type": "IMAGE"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":"image"});
        }

        if (entity["fs:location"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:location"], "type": "LOC"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("location")});
        }

        if (entity["fs:country"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:country"], "type": "LOC"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("country")});
        }
        graph["nodes"].push({"name":entity["fs:source"],"type":"LITERAL"});
        graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("source")});

        if (entity["fs:price"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:price"], "type": "LITERAL"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("price")});
        }
        if (entity["fs:condition"] !== undefined) {
            graph["nodes"].push({"name": entity["fs:condition"], "type": "LITERAL"});
            graph["links"].push({"source":0,"target":graph["nodes"].length - 1,"label":getTranslation("condition")});
        }
        return graph;
    },
    openOverlayScreen: function(){
        document.getElementById("overlayScreen" + this.props.id).style.width = "100%";

        var el = "#" + this.props.id;
        var graph = this.convertData(this.props.entity);
        var graph_Height = $("#overlayScreen" + this.props.id).height();
        d3Graph.create(el, {
            width: graph_Height*2,
            height: graph_Height,
            charge : -700,
            linkDist : 350,
            linkStrength : 0.7,
            gravity : 0.04
        }, graph);
    },
    closeOverlayScreen: function(){
        document.getElementById("overlayScreen" + this.props.id).style.width = "0%";
        d3Graph.destroy();
    },
    render: function () {
        return(
            <div>
                <button onClick={this.openOverlayScreen}>&nbsp;Show Graph&nbsp;</button>
                <div className ="overlay" id = {"overlayScreen"+this.props.id}>
                    <a href="javascript:void(0)" className="closebtn" onClick={this.closeOverlayScreen}>&times;</a>
                    <div className="works rolefacet" id={this.props.id}>
                    </div>
                </div>
            </div>
        );
    }
});

var RetunToTopBtn =  React.createClass({
   onClickHandle: function(){
       $('body,html').animate({
           scrollTop : 0                       // Scroll to top of body
       }, 500);
   },
    onScrollHandle: function(){
        if ($(window).scrollTop() >= 50) {        // If page is scrolled more than 50px
            $('#return-to-top').fadeIn(200);    // Fade in the arrow
        } else {
            $('#return-to-top').fadeOut(200);   // Else fade out the arrow
        }
    },
    componentDidMount: function(){
        document.addEventListener('scroll',this.onScrollHandle);
    },
    componentWillUnmount: function() {
        document.removeEventListener('scroll', this.onScrollHandle);
    },
    render : function () {
       return(
           <a href="javascript:" id="return-to-top" title="Return to top"  onClick={this.onClickHandle}><i className="fa fa-angle-double-up"></i></a>
       )
   } 
});