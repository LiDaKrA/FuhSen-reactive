var FavoritesContainer = React.createClass({
    getInitialState: function () {
        return {data: undefined};
    },
    componentWillMount: function(searchUid){
        var urlString = window.location.href.toString();
        var parts = urlString.split('=');
        var searchUid = parts[1];
        var ref = this;
        let url = context + '/' + searchUid + '/favorites';
        $.ajax({
            url: url,
            cache: false,
            type: 'GET',
            success: function(response) {
                var obj = JSON.parse(response);
                var objGraph = undefined;
                if (obj["@graph"] !== undefined) {
                    //alert("It contains Graph");
                    objGraph = obj["@graph"];
                }
                else {
                    //alert("Looking for id");
                    //if (obj["@id"] !== undefined) {
                        //alert("Id was found");
                        var obj2 = JSON.parse("{ \"@graph\": [" + JSON.stringify(response) + "]}");
                        objGraph = obj2["@graph"];
                    //}
                }
                console.log("Graph: "+obj);
                ref.setState({searchUid: searchUid, data: objGraph});
            },
            error: function(xhr) {
                console.log(xhr)
            }
        });
    },
    handleClean: function ()
    {
        var urlString = window.location.href.toString();
        var parts = urlString.split('=');
        var searchUid = parts[1];
        let url = context + '/' + searchUid + '/favorites/clean';
        var ref = this;
        $.ajax({
            url: url,
            cache: false,
            type: 'GET',
            success: function(response) {
                console.log("Response Cleaning Favorites: "+response);
                ref.setState({searchUid: searchUid, data: null});
            },
            error: function(xhr) {
                console.log(xhr)
            }
        });
    },
    render: function () {
        var resultList = this.state.data;
        var results = {};
        if(resultList !== undefined){
            results = resultList.map(function (result,idx) {
                return (
                    <FavouriteResult
                        uri={result["@id"]}
                        id={result["http://vocab.lidakra.de/fuhsen#id"]}
                        img={result.img}
                        name={result["http://vocab.lidakra.de/fuhsen#name"]}
                        source={result["http://vocab.lidakra.de/fuhsen#source"]}
                        alias={result["http://vocab.lidakra.de/fuhsen#alias"]}
                        location={result["http://vocab.lidakra.de/fuhsen#location"]}
                        label={result["http://www.w3.org/2000/01/rdf-schema#label"]}
                        webpage={result["website"]}
                        uid ={this.state.searchUid}>
                    </FavouriteResult>
                );
            }, this);
        }

        return (
            <div className="favContainer">
                <div>
                    <h1>List of Favourites</h1>
                    {results}
                </div>
                <div>
                    <span className="btn btn-primary btn-file btn-md">
                    {getTranslation("select_file")} <input type="file"></input>
                    </span>
                    &nbsp;&nbsp;
                    <span className="btn btn-primary btn-file btn-md">
                    {getTranslation("clean_favorites")} <input type="file" onChange={this.handleClean}></input>
                    </span>
                </div>
            </div>

        );
    }
});

var FavouriteResult = React.createClass({
    getInitialState: function () {
        return {data: undefined};
    },
    render: function () {
        var detailsPageUri = context + "/details?entityType=person" + "&eUri=" + this.props.uri + "&uid=" + this.props.uid;
        return (
            <li className="item fav-item bt">
                <div className="summary row">
                    <div className="thumbnail-wrapper col-md-2">
                        <div className="thumbnail">
                            { this.props.img !== undefined ? <img src={this.props.img} height="60px" width="75px"/> :
                                <img src={context + "/assets/images/datasources/Unknown.png"} height="60px"
                                     width="75px"/> }
                        </div>
                    </div>
                    <div className="summary-main-wrapper col-md-8">
                        <div className="summary-main">
                            <a href={detailsPageUri} target="_blank">
                                <h2 className="title">
                                    {this.props.name}
                                </h2>
                            </a>
                            <div className="subtitle">
                                { this.props.alias !== undefined ?
                                    <p>{getTranslation("nick")}: {this.props.alias}</p> : null }
                                { this.props.location !== undefined ?
                                    <p>{getTranslation("location")}: {this.props.location}</p> : null }
                                { this.props.gender !== undefined ?
                                    <p>{getTranslation("gender")}: {this.props.gender}</p> : null }
                                { this.props.occupation !== undefined ?
                                    <p>{getTranslation("occupation")}: {this.props.occupation}</p> : null }
                                { this.props.birthday !== undefined ?
                                    <p>{getTranslation("birthday")}: {this.props.birthday}</p> : null }
                                { this.props.country !== undefined ?
                                    <p>{getTranslation("country")}: {this.props.country}</p> : null }
                                { this.props.label !== undefined ? <p>{this.props.label}</p> : null }
                                { this.props.comment !== undefined ? <p>{this.props.comment}</p> : null }
                                { this.props.webpage !== undefined ?
                                    <p><b>{getTranslation("link")}: </b>
                                        <a href={this.props.webpage} target="_blank">{this.props.webpage}</a></p>
                                    : null }
                                { this.props.active_email !== undefined ?
                                    <p><b>{getTranslation("active_email")}:</b> {this.props.active_email}</p> : null }
                                { this.props.wants !== undefined ?
                                    <p><b>{getTranslation("wants")}:</b> {this.props.wants}</p> : null }
                                { this.props.haves !== undefined ?
                                    <p><b>{getTranslation("haves")}:</b> {this.props.haves}</p> : null }
                                { this.props.top_haves !== undefined && this.props.top_haves !== "null" ?
                                    <p><b>{getTranslation("top_haves")}:</b> {this.props.top_haves}</p> : null }
                                { this.props.interests !== undefined ?
                                    <p><b>{getTranslation("interests")}:</b> {this.props.interests}</p> : null }
                            </div>
                        </div>
                    </div>
                    <div class="thumbnail-wrapper col-md-1">
                        <div className="thumbnail">
                            <img src={context + "/assets/images/datasources/" + this.props.source + ".png"}
                                 alt={"Information from " + this.props.source} height="45" width="45"
                                 title={this.props.source}/>
                        </div>
                    </div>
                </div>
            </li>
        );
    }
});

React.render(
    <FavoritesContainer/>, document.getElementById('skeleton'));