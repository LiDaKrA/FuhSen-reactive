var Trigger = React.createClass({
    loadKeywordFromServer: function () {
        $.ajax({
            url: this.props.url,
            dataType: 'json',
            cache: false,
            success: function (kw) {
                this.setState({keyword: kw["keyword"]});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    getInitialState: function () {
        return {keyword: null};
    },
    componentDidMount: function () {
        this.loadKeywordFromServer();
        //setInterval(this.loadKeywordFromServer, this.props.pollInterval);
    },
    render: function () {
        if (this.state.keyword) {
            return ( <Container keyword={this.state.keyword} pollInterval={200000}/>);
        }
        return <div>Loading...</div>;
    }
});

var Container = React.createClass({
    loadCommentsFromServer: function () {

        var searchUrl = "/ldw/v1/restApiWrapper/id/twitter/search?query="+this.props.keyword;

        $.ajax({
            url: searchUrl,
            dataType: 'json',
            cache: false,
            success: function (data) {
                this.setState({data: data});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    getInitialState: function () {
        return {data: null};
    },
    componentDidMount: function () {
        this.loadCommentsFromServer();
        setInterval(this.loadCommentsFromServer, this.props.pollInterval);
    },
    render: function () {
        if (this.state.data) {
            return ( <div>
                        <Facets></Facets>
                        <ResultsContainer data={this.state.data}></ResultsContainer>
                    </div>);
        }
        return <div>Loading...</div>;
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
    render: function () {
        var resultsNodes = this.props.data["@graph"].map(function (result) {
            return (
                <ResultElement img={result.img}
                               webpage={result.url}
                               name={result["http://xmlns.com/foaf/0.1/name"]}
                               location={result["http://vocab.cs.uni-bonn.de/fuhsen#location"]}
                               alias={result["http://vocab.cs.uni-bonn.de/fuhsen#alias"]}
                               social_url="http://localhost:9000/assets/images/datasources/facebook.png">
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
                                {this.props.name}
                            </h2>
                            <div className="subtitle">
                                <p>Nick: {this.props.alias}</p>
                                <p>Location: {this.props.location}</p>
                                <p>Webpage: {this.props.webpage}</p>
                            </div>
                        </div>
                    </div>
                    <div class="thumbnail-wrapper col-md-1">
                        <div class="thumbnail">
                            <img src={this.props.social_url} alt="Information from FB" height="45" width="45"/>
                        </div>
                    </div>
                </div>
            </li>
        );
    }
});

React.render(<Trigger url="/keyword" pollInterval={200000}/>, document.getElementById('skeleton'));
React.render(<SearchForm id_class="form-search-header"/>, document.getElementById('searchform'));