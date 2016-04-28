var Container = React.createClass({
    loadCommentsFromServer: function() {
        $.ajax({
            url: this.props.url,
            dataType: 'json',
            cache: false,
            success: function(data) {
                this.setState({data: data});
            }.bind(this),
            error: function(xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    getInitialState: function() {
        return {data: []};
    },
    componentDidMount: function() {
        this.loadCommentsFromServer();
        setInterval(this.loadCommentsFromServer, this.props.pollInterval);
    },
    render: function () {
        return (
            <div className="row search-results-container">
                <Facets></Facets>
                <ResultsContainer data={this_data}></ResultsContainer>
            </div>
        );
    }
});

var Facets = React.createClass({
    render: function () {
        return (
            <div className="col-md-3 facets-container hidden-phone">
                <div className="facets-head">
                    <h3>Ergebnisse filtern</h3>
                </div>
                <div className="js facets-list bt bb">
                    <div className="facets-item bt bb bl br">
                        <a className="h3" href="#" data-fctname="person_gender_fct">Geschlecht</a>
                    </div>
                    <div className="facets-item bt bb bl br">
                        <a className="h3" href="#" data-fctname="person_birthday_fct">Geburtstag</a>
                    </div>

                    <div className="facets-item bt bb bl br">
                        <a className="h3" href="#" data-fctname="person_occupation_fct">Beruf</a>
                    </div>

                    <div className="facets-item bt bb bl br">
                        <a className="h3" href="#" data-fctname="person_livesat_fct">Lebt in</a>
                    </div>

                    <div className="facets-item bt bb bl br">
                        <a className="h3" href="#" data-fctname="person_worksat_fct">Arbeitet bei</a>
                    </div>

                    <div className="facets-item bt bb bl br">
                        <a className="h3" href="#" data-fctname="person_studiesat_fct">Studium an</a>
                    </div>
                </div>
            </div>
        );
    }
});

var ResultsContainer = React.createClass({
    render: function () {
        return (
                <div className="col-md-9">
                    <div id="results-paginator-options" className="results-paginator-options">
                        <div class="off result-pages-count"></div>
                        <div className="row">
                            <div className="col-md-12">
                                <ul className="tabulator list-inline">
                                    <li>
                                        <span className="total-results">50</span>
                                        <span className="total-results-label"> Ergebnisse:</span>
                                    </li>
                                    <li>
                                        <a href="#" className="active-link">
                                            <p><strong>Personen</strong></p>
                                        </a>
                                    </li>
                                    <li>
                                        <a href="#" className="">
                                            Organisationen
                                        </a>
                                    </li>
                                    <li>
                                        <a href="#" className="">
                                            Produkte
                                        </a>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    <div className="search-results-content">
                        <div className="row">
                            <div className="col-md-12">
                                <ul id="search-results" className="search-results">
                                    <ul className="results-list list-unstyled">
                                        <ResultsList data={this.props.data}>
                                        </ResultsList>
                                    </ul>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
        );
    }
});

var ResultsList = React.createClass({
    render: function() {
        var resultsNodes = this.props.data["@graph"].map(function (result) {
            return (
                <ResultElement img={result.img} url={result.url}>
                </ResultElement>
            );
        });

        return (
            <div className="commentList">
                {resultsNodes}
            </div>
        );
    }
});

var ResultElement = React.createClass({
    render: function () {
        return (
            <li className="item bt">
                <div className="summary row">
                    <div className="thumbnail-wrapper col-md-2">
                        <div className="thumbnail">
                            <img src={this.props.img} height="60px" width="75px"/>
                        </div>
                    </div>
                    <div className="summary-main-wrapper col-md-8">
                        <div className="summary-main">
                            <h2 className="title">
                                Some ID
                            </h2>
                            <div className="subtitle">
                                <p>Gender: Some Gender.</p>
                                <p>Address: Some Address.</p>
                            </div>
                        </div>
                    </div>
                    <div class="thumbnail-wrapper col-md-1">
                        <div class="thumbnail">
                            <img src={this.props.url} alt="Information from FB" height="45" width="45"/>
                        </div>
                    </div>
                </div>
            </li>
        );
    }
});

var this_data={ "@graph" : [
    {	"http://vocab.cs.uni-bonn.de/fuhsen#alias":"Diego Collarana",
        "gender":"Male",
        "addrss":[ "http://vocab.cs.uni-bonn.de/fuhsen#SearchableEntity", "http://xmlns.com/foaf/0.1/Person" ],
        "img":"https://pbs.twimg.com/profile_images/2175489201/Dic01.png",
        "url":"http://localhost:9000/assets/images/datasources/googleplus.png"
    },
    {
        "http://vocab.cs.uni-bonn.de/fuhsen#alias":"Camilo Morales",
        "gender":"Male",
        "addrss": [ "http://vocab.cs.uni-bonn.de/fuhsen#SearchableEntity", "http://xmlns.com/foaf/0.1/Person" ],
        "img":"https://scontent.xx.fbcdn.net/hprofile-xpa1/v/t1.0-1/p50x50/21171_10155422530195055_1584223177974390414_n.jpg?oh=87385fa8dc9a98da04bb2b135292a74f&oe=57C036F3",
        "url":"http://localhost:9000/assets/images/datasources/facebook.png"
    }
],
    "@context" : {
        "alias" : {
            "@id" : "http://vocab.cs.uni-bonn.de/fuhsen#alias",
            "@type" : "http://www.w3.org/2001/XMLSchema#string"
        },
        "id" : {
            "@id" : "http://vocab.cs.uni-bonn.de/fuhsen#id",
            "@type" : "http://www.w3.org/2001/XMLSchema#double"
        },
        "lang" : {
            "@id" : "http://vocab.cs.uni-bonn.de/fuhsen#lang",
            "@type" : "http://www.w3.org/2001/XMLSchema#string"
        },
        "location" : {
            "@id" : "http://vocab.cs.uni-bonn.de/fuhsen#location",
            "@type" : "http://www.w3.org/2001/XMLSchema#string"
        },
        "url" : {
            "@id" : "http://vocab.cs.uni-bonn.de/fuhsen#url",
            "@type" : "@id"
        },
        "img" : {
            "@id" : "http://xmlns.com/foaf/0.1/img",
            "@type" : "@id"
        },
        "name" : {
            "@id" : "http://xmlns.com/foaf/0.1/name",
            "@type" : "http://www.w3.org/2001/XMLSchema#string"
        }
    }
};

React.render(<Container url="/ldw/v1/restApiWrapper/id/twitter/search?query=Camilo" pollInterval={200000}/>, document.getElementById('skeleton'));
React.render(<SearchForm id_class="form-search-header"/>, document.getElementById('searchform'));