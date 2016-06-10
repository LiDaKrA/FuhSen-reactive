checkLanguage();

var facetsStaticData = [
    {name:'gender',elements:['male', 'female'],results:[2,8]},
    {name:'birthday',elements:['1.1.1900'],results:[1]},
    {name:'occupation',elements:['I am a fresh so yeah'],results:[3]},
    {name:'livein',elements:['Bonn', 'Koeln'],results:[1,3]},
    {name:'workfor',elements:['Google', 'Fraunhofer'],results:[1,1]},
    {name:'studies',elements:['Uni Bonn', 'Uni Berlin'],results:[3,7]}
];

var ContainerResults = React.createClass({
    // event handler for language switch
    // change dictionary then update state so the page notices the change
    setLang: function () {
        var lang = document.getElementById("langselect").value;
        switch (lang) {
            case "german":
                window.globalDict = dictGer;
                window.localStorage.lang = "ger";
                this.setState({dictionary: "ger"});
                globalFlushFilters();
                break;
            case "english":
                window.globalDict = dictEng;
                window.localStorage.lang = "eng";
                this.setState({dictionary: "eng"});
                globalFlushFilters();
                break;
        }
    },
    render: function () {
        return (
            <div>
                <div className="container">
                    <div className="row" id="header-main-row">
                        <nav className="widget col-md-12" data-widget="NavigationWidget">
                            <div className="row">
                                <div className="col-md-7">
                                    <a href="/">
                                        <img src="/assets/images/logoBig2.png" class="smallLogo" alt="Logo_Description"/>
                                    </a>
                                </div>
                                <div className="col-md-5 toolbar search-header hidden-phone text-right">
                                    <LangSwitcher onlangselect={this.setLang}/>

                                    <SearchForm id_class="form-search-header"/>
                                </div>
                            </div>
                        </nav>
                    </div>
                </div>

                <div className="row search-results-container">
                    <Trigger facetsData={facetsStaticData} url="/keyword" pollInterval={200000}/>
                </div>
            </div>
        );
    }
});

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
            return ( <Container facetData={this.props.facetsData} keyword={this.state.keyword} pollInterval={200000}/>);
        }
        return <div className="row">
            <div className="col-md-12">
                <h2>Bitte warten Sie, während die Ergebnisse laden...</h2>
                <img className="img-responsive center-block" src="/assets/images/ajaxLoading.gif" alt="Loading results"/>
            </div>
        </div>;
    }
});

var Container = React.createClass({
    loadCommentsFromServer: function () {

        var searchUrl = "/ldw/restApiWrapper/id/twitter/search?query="+this.props.keyword;

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
            return ( <div class="row search-results-container">
                <FacetList facetData={this.props.facetData}/>
                <ResultsContainer data={this.state.data} keyword={this.props.keyword}></ResultsContainer>
            </div>);
        }
        return <div className="row">
            <div className="col-md-12 text-center">
                <img className="img-responsive center-block" src="/assets/images/ajaxLoading.gif" alt="Loading results"/>
                <h2><img src="/assets/images/ajaxLoader.gif"/>{getTranslation("bittewarten")}</h2>
            </div>
        </div>;
    }
});


//************** Begin Facets Components *******************

var FacetItems = React.createClass({
    getInitialState: function() {
        arr_ele = [];//fill elements of the sub menu in an array
        return  { showTextBox: false };
    },
    onClick: function() {
        var propsName = this.props.name.replace(/\s/g, '');
        var propsName_key = arr_ele.indexOf(propsName);

        //Check if the menu item is shown
        // if Yes hide it, if No show it
        if(this.state.showTextBox){

            //Check if the item is in the array: means you just now clicked it, then hide it by setting the state to false and remove it from the array
            //if not in the array: means it was hidden by showing other item:
            //      - then show it by using normal js
            //      - set the state to true
            //      - hide and remove all others
            if( propsName_key >= 0 ){
                this.setState({ showTextBox: false });
                arr_ele.splice(propsName_key, 1);
            }
            else{
                this.setState({ showTextBox: true });
                arr_ele.push(propsName);
                document.getElementById(propsName).style.display = "inline";
                if(arr_ele[0] != propsName){
                    document.getElementById(arr_ele[0]).style.display = "none";
                    arr_ele.splice(0, 1);
                }

            }
        }
        else{
            this.setState({ showTextBox: true });
            if(propsName_key < 0){
                arr_ele.push(propsName);
            }
            for(var i = 0; i<arr_ele.length-1; i++){
                if (arr_ele[i] != propsName){
                    document.getElementById(arr_ele[i]).style.display = "none";
                    arr_ele.splice(arr_ele[i], 1);
                }
            }
        }
    },
    render: function () {
        var propsName = this.props.name.replace(/\s/g, '') ;
        return (
            <div className="facets-item bt bb bl br" >
                <a className="h3" href="#" onClick={this.onClick}>{this.props.name}</a>
                <div id={""+propsName+""}>
                    { this.state.showTextBox ? <FacetSubMenuItems elements={this.props.elements} results={this.props.results}/> : null }
                </div>
            </div>

        );
    }
});

var FacetSubMenuItems = React.createClass({
    render: function () {
        var subMenuEle = [];
        for (var i = 0; i < this.props.elements.length; i++) {
            subMenuEle.push(<li ><a href="#" onClick={this.onClick}><span className="sub-item">{this.props.elements[i]}</span><span className="sub-item-result">({this.props.results[i]})</span></a></li>);
        }
        return (
            <div>
                <div className="flyout-left-container">
                    <ul className="selected-items unstyled"></ul>
                    <div className="input-search-fct-container">
                        <input type="text" className="input-search-fct"/>
                    </div>
                </div>

                <div className="flyout-right-container" >
                    <div className="flyout-right-head">
                        <span>{getTranslation("sortedby")}</span>
                        <div className="flyout-page-nav fr">
                            <ul className="inline">
                                <li className="pages-overall-index">{getTranslation("page")}<span>1</span></li>
                            </ul>
                        </div>
                    </div>
                    <div className="flyout-right-body" >
                        <ul className="left-col unstyled">
                            {subMenuEle}
                        </ul>
                    </div>
                </div>
            </div>
        );
    }
});

// inject/ passing data
var FacetList = React.createClass({
    render: function () {
        var MItems = this.props.facetData.map(function(menuItems){
            return <FacetItems name={getTranslation(menuItems.name)} elements={menuItems.elements} results={menuItems.results}/>
        });
        return (
            <div className="col-md-3 facets-container hidden-phone">
                <div className="facets-head">
                    <h3>{getTranslation("resultfilters")}</h3>
                </div>
                <div className="js facets-list bt bb">
                    {MItems}
                </div>
            </div>
        )
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

//************** End Facets Components *******************

var ResultsContainer = React.createClass({
    getInitialState: function () {
        return {new_data:"", selected: "1", loading: false};
    },
    onItemClick: function (event) {
        var optionSelected = event.currentTarget.dataset.id;

        this.setState({new_data : "", selected : optionSelected, loading: true});

        var searchUrl = "/ldw/restApiWrapper/id/twitter/search?query=";
        var type;

        if(optionSelected==="1") {
            type = "Camilo"
        } else if(optionSelected==="2"){
            type = "Diego"
        } else if(optionSelected==="3"){
            type = "Luigi"
        } else if(optionSelected==="4"){
            searchUrl = "/ldw/restApiWrapper/id/tor2web/search?query="
            type = "Obama"
        }

        searchUrl = searchUrl+type

        $.ajax({
            url: searchUrl,
            dataType: 'json',
            cache: false,
            success: function (data) {
                this.setState({new_data : data, selected : optionSelected, loading: false});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    render: function(){

        var personenItem = <li onClick={this.onItemClick} data-id="1">{getTranslation("people")}</li>
        var organizationenItem = <li onClick={this.onItemClick} data-id="2">{getTranslation("organisations")}</li>
        var produkteItem = <li onClick={this.onItemClick} data-id="3">{getTranslation("products")}</li>
        var darkWebItem = <li onClick={this.onItemClick} data-id="4">Websites</li>

        if(this.state.selected==="1") {
            personenItem = <li onClick={this.onItemClick} data-id="1"><p><b>{getTranslation("people")}</b></p></li>
        } else if(this.state.selected==="2"){
            organizationenItem = <li onClick={this.onItemClick} data-id="2"><p><b>{getTranslation("organisations")}</b></p></li>
        } else if(this.state.selected==="3"){
            produkteItem = <li onClick={this.onItemClick} data-id="3"><p><b>{getTranslation("products")}</b></p></li>
        } else if(this.state.selected==="4"){
            darkWebItem = <li onClick={this.onItemClick} data-id="$"><p><b>Websites</b></p></li>
        }

        if (this.state.loading) {
            return <div className="col-md-9">
                <div id="results-paginator-options" className="results-paginator-options">
                    <div class="off result-pages-count"></div>
                    <div className="row">
                        <div className="col-md-8 tabulator">
                            <ul className="list-inline">
                                <li>
                                    <span className="total-results-label"> {getTranslation("results")}:</span>
                                </li>
                                {personenItem}
                                {organizationenItem}
                                {produkteItem}
                                {darkWebItem}
                            </ul>
                        </div>
                        <div className="col-md-4">
                            <CSVForm data={final_data}></CSVForm>
                        </div>
                    </div>
                </div>

                <div className="row">
                    <div className="col-md-12 text-center">
                        <img className="img-responsive center-block" src="/assets/images/ajaxLoading.gif" alt="Loading results"/>
                        <h2><img src="/assets/images/ajaxLoader.gif"/>{getTranslation("bittewarten")}</h2>
                    </div>
                </div>
            </div>;
        }

        var final_data;

        if(this.state.new_data === "") {
            final_data = this.props.data
        } else {
            final_data = this.state.new_data
        }

        return <div className="col-md-9">
            <div id="results-paginator-options" className="results-paginator-options">
                <div class="off result-pages-count"></div>
                <div className="row">
                    <div className="col-md-8 tabulator">
                        <ul className="list-inline">
                            <li>
                                <span className="total-results">{final_data["@graph"].length}</span>
                                <span className="total-results-label"> {getTranslation("results")}:</span>
                            </li>
                            {personenItem}
                            {organizationenItem}
                            {produkteItem}
                            {darkWebItem}
                        </ul>
                    </div>
                    <div className="col-md-4 text-right">
                        <CSVForm data={final_data}></CSVForm>
                    </div>
                </div>
            </div>
            <div className="search-results-content">
                <div className="row">
                    <div className="col-md-12">
                        <ul id="search-results" className="search-results">
                            <ul className="results-list list-unstyled">
                                <ResultsList data={final_data}>
                                </ResultsList>
                            </ul>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    }
});

var CSVForm = React.createClass({
    handleClick: function(e) {

        var JSONData = JSON.stringify(this.props.data["@graph"]);
        var ReportTitle = "Current results in CSV format"
        var ShowLabel = true;

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
                {getTranslation("exporttocsv")}
            </button>
        );
    }
});

var ResultsList = React.createClass({
    render: function () {
        var resultsNodes = this.props.data["@graph"].map(function (result) {

            if(result["http://vocab.cs.uni-bonn.de/fuhsen#source"] === undefined){
                return (
                    <ResultElement
                        img={result.img}
                        webpage={result.url}
                        name={result["http://xmlns.com/foaf/0.1/name"]}
                        location={result["http://vocab.cs.uni-bonn.de/fuhsen#location"]}
                        alias={result["http://vocab.cs.uni-bonn.de/fuhsen#alias"]}
                        social_url="/assets/images/datasources/facebook.png">
                    </ResultElement>
                );
            } else {
                return (
                    <DWResultElement
                        img="/assets/images/Tor_project_logo_hq.png"
                        onion_url={result.url}
                        comment={result["http://www.w3.org/2000/01/rdf-schema#comment"]}
                        social_url="/assets/images/Tor_logo1.png">
                    </DWResultElement>
                );
            }
        });

        return (
            <div className="commentList">
                {resultsNodes}
            </div>
        );
    }
});

var DWResultElement = React.createClass({
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
                                {this.props.onion_url}
                            </h2>
                            <div className="subtitle">
                                <p><b>{getTranslation("comment")}</b>: {this.props.comment}</p>
                                <p><b>Link: </b>: <a href={this.props.onion_url} target="_blank">{getTranslation("clickhere")}</a></p>
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
                                <p>{getTranslation("nick")}: {this.props.alias}</p>
                                <p>{getTranslation("location")}: {this.props.location}</p>
                                <p>{getTranslation("webpage")}: {this.props.webpage}</p>
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

React.render(<ContainerResults facetsData={facetsStaticData} url="/keyword" pollInterval={200000}/>, document.getElementById('skeleton'));