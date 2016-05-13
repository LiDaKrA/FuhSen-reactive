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
        return <div className="row">
                    <div className="col-md-12">
                        <h2>Bitte warten Sie, während die Ergebnisse laden...</h2>
                        <img className="img-responsive center-block" src="http://localhost:9000/assets/images/ajaxLoading.gif" alt="Loading results"/>
                    </div>
               </div>;
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
        return <div className="row">
                    <div className="col-md-12">
                        <h2>Bitte warten Sie, während die Ergebnisse laden...</h2>
                        <img className="img-responsive center-block" src="http://localhost:9000/assets/images/ajaxLoading.gif" alt="Loading results"/>
                    </div>
               </div>;
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
                        <div className="col-md-10">
                            <ul className="tabulator list-inline">
                                <li>
                                    <span className="total-results">10</span>
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
                        <div className="col-md-2">
                            <CSVForm data={this.props.data}></CSVForm>
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

var CSVForm = React.createClass({
    handleClick: function(e) {

        var JSONData = JSON.stringify(this.props.data["@graph"]);
        var ReportTitle = "Current results in CSV format"
        var ShowLabel = false;

        //If JSONData is not an object then JSON.parse will parse the JSON string in an Object
        var arrData = typeof JSONData != 'object' ? JSON.parse(JSONData) : JSONData;

        var CSV = '';
        //Set Report title in first row or line

        CSV += ReportTitle + '\r\n\n';

        //This condition will generate the Label/Header
        if (ShowLabel) {
            var row = "";

            //This loop will extract the label from 1st index of on array
            for (var index in arrData[0]) {

                //Now convert each value to string and comma-seprated
                row += index + ',';
            }

            row = row.slice(0, -1);

            //append Label row with line break
            CSV += row + '\r\n';
        }

        //1st loop is to extract each row
        for (var i = 0; i < arrData.length; i++) {
            var row = "";

            //2nd loop will extract each column and convert it in string comma-seprated
            for (var index in arrData[i]) {
                row += '"' + arrData[i][index] + '",';
            }

            row.slice(0, row.length - 1);

            //add a line break after each row
            CSV += row + '\r\n';
        }

        if (CSV == '') {
            alert("Invalid data");
            return;
        }

        //Generate a file name
        var fileName = "Fuhsen_";
        //this will remove the blank-spaces from the title and replace it with an underscore
        fileName += ReportTitle.replace(/ /g,"_");

        //Initialize file format you want csv or xls
        var uri = 'data:text/csv;charset=utf-8,' + escape(CSV);

        // Now the little tricky part.
        // you can use either>> window.open(uri);
        // but this will not work in some browsers
        // or you will not get the correct file extension

        //this trick will generate a temp <a /> tag
        var link = document.createElement("a");
        link.href = uri;

        //set the visibility hidden so it will not effect on your web-layout
        link.style = "visibility:hidden";
        link.download = fileName + ".csv";

        //this part will append the anchor tag and remove it after automatic click
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    },
    render: function() {
        return (
            <button onClick={this.handleClick}>
                Export to CSV
            </button>
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