checkLanguage();

var context = $('body').data('context')

function extractQuery(key) {
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split("=");
        if (pair[0] == key) {
            return decodeURIComponent(pair[1]);
        }
    }
    return (false);
}
var queryDirty = extractQuery("query");
var query = queryDirty.replace(new RegExp('\\+', 'g'), ' ');

function compareRank(a, b) {
    if (a["fs:rank"] < b["fs:rank"])
        return -1;
    if (a["fs:rank"] > b["fs:rank"])
        return 1;
    return 0;
}

var sourcesDirty = extractQuery("sources");
var typesDirty = extractQuery("types");

var ContainerResults = React.createClass({
    // event handler for language switch
    // change dictionary then update state so the page notices the change
    setLang: function () {
        switch (window.localStorage.getItem("lang")) {
            case "de":
                window.globalDict = dictGer;
                window.localStorage.lang = "de";
                this.setState({dictionary: "de"});
                globalFlushFilters();
                break;
            case "en":
                window.globalDict = dictEng;
                window.localStorage.lang = "en";
                this.setState({dictionary: "en"});
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
                                <div className="col-md-4">
                                    <a href={context === "" ? "/" : context}>
                                        <img src={context + "/assets/images/logoBig2.png"} class="smallLogo"
                                             alt="Logo_Description"/>
                                    </a>
                                </div>
                                <div className="col-md-3">
                                </div>
                                <div className="col-md-5 toolbar search-header hidden-phone text-right">
                                    <div className="row">
                                        <div className="col-md-12">
                                            <LangSwitcher onlangselect={this.setLang}/>
                                            <SearchForm id_class="form-search-header" keyword={query}/>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </nav>
                    </div>
                </div>

                <div className="row search-results-container">
                    <Trigger url={context + "/engine/api/searches?query=" + query} pollInterval={200000}/>
                </div>

                <a href="http://www.bdk.de/lidakra" target="_blank" className="no-external-link-icon">
                    <div id="logo-mini" title={getTranslation("sponsored_by")}/>
                </a>

            </div>
        );
    }
});

var Trigger = React.createClass({
    loadKeywordFromServer: function () {
        $.ajax({
            url: this.props.url,
            dataType: 'json',
            type: "POST",
            cache: false,
            success: function (kw) {
                this.setState({keyword: kw["keyword"], searchUid: kw["uid"]});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    getInitialState: function () {
        return {keyword: null, searchUid: null};
    },
    componentDidMount: function () {
        this.loadKeywordFromServer();
        //setInterval(this.loadKeywordFromServer, this.props.pollInterval);
    },
    render: function () {
        if (this.state.keyword) {
            return ( <Container keyword={this.state.keyword} searchUid={this.state.searchUid}/>);
        }

        return <div className="row">
            <div className="col-md-12 text-center">
                <img className="img-responsive center-block" src={context + "/assets/images/ajaxLoading.gif"}
                     alt="Loading results"/>
                <h2><img src={context + "/assets/images/ajaxLoader.gif"}/>{getTranslation("bittewarten")}</h2>
            </div>
        </div>;
    }
});

var Container = React.createClass({
    onFacetSelection: function (facetName, propertyName,facetValue) {
        if (this.state.facetsDict[facetName]) {
            if (this.state.facetsDict[facetName].indexOf(facetValue) === -1) {
                this.state.facetsDict[facetName].push(facetValue)
                this.state.orgFacetsDict[propertyName].push(facetValue)
                this.setState({facetsDict: this.state.facetsDict,orgFacetsDict: this.state.orgFacetsDict})
            }
        } else {
            this.state.facetsDict[facetName] = [facetValue];
            this.state.orgFacetsDict[propertyName] = [facetValue];
            this.setState({facetsDict: this.state.facetsDict,orgFacetsDict: this.state.orgFacetsDict})
        }
    },
    onFacetRemoval: function (facetName, propertyName,facetValue) {
        if (facetValue != "all") {
            var index_of = this.state.facetsDict[facetName].indexOf(facetValue)
            this.state.facetsDict[facetName].splice(index_of, 1)
            index_of = this.state.orgFacetsDict[propertyName].indexOf(facetValue)
            this.state.orgFacetsDict[propertyName].splice(index_of, 1)

            if (this.state.facetsDict[facetName].length === 0) {
                delete this.state.facetsDict[facetName]
                delete this.state.orgFacetsDict[propertyName]
            }
        } else {
            delete this.state.facetsDict[facetName]
            delete this.state.orgFacetsDict[propertyName]
        }

        this.setState({facetsDict: this.state.facetsDict,orgFacetsDict: this.state.orgFacetsDict})
    },
    loadCommentsFromServer: function () {
        var searchUrl = context + "/engine/api/searches/" + this.props.searchUid + "/results?entityType=" + this.state.entityType + "&sources=" + sourcesDirty + "&types=" + typesDirty + "&exact=" + this.state.exactMatching;
        $.ajax({
            url: searchUrl,
            dataType: 'json',
            cache: false,
            success: function () {
                this.setState({initData: true});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());

                if(err.toString().includes("Not Acceptable")){
                    alert(getTranslation("no_valid_token_found"))
                }else if(err.toString().includes("timeout")){
                    alert(getTranslation("timeout"));
                }else{
                    alert(getTranslation("internal_server_error"));
                }

                //Todo remove this hardcoded value
                window.location.href = "/fuhsen";
            }.bind(this)
            ,timeout: 120000 // sets timeout to 120 seconds
        });
    },
    componentDidMount: function () {
        this.loadCommentsFromServer();
    },
    getInitialState: function () {
        return {view: "list", entityType: "person", facets: "", initData: false, facetsDict: {}, orgFacetsDict: {}, exactMatching:false};
    },
    onExactMatchingChange: function () {
        this.setState({exactMatching:!this.state.exactMatching})
    },
    onTypeChange: function (event) {
        var optionSelected = event.currentTarget.dataset.id;
        var type;
        if (optionSelected === "1") {
            type = "person"
        } else if (optionSelected === "2") {
            type = "organization"
        } else if (optionSelected === "3") {
            type = "product"
        } else if (optionSelected === "4") {
            type = "website"
        } else if (optionSelected === "5") {
            type = "document"
        }
        this.setState({entityType: type,facetsDict: {}, orgFacetsDict: {}});
    },
    render: function () {
        if (this.state.initData) {
            return (<div class="row search-results-container">
                <FacetList searchUid={this.props.searchUid}
                           keyword={this.props.keyword}
                           entityType={this.state.entityType}
                           onFacetSelection={this.onFacetSelection}
                           onFacetRemoval={this.onFacetRemoval}
                           currentTab={this.state.entityType}
                           orgFacetsDict = {this.state.orgFacetsDict}
                           onExactMatchingChange = {this.onExactMatchingChange}
                           exactMatching={this.state.exactMatching}/>
                <ResultsContainer searchUid={this.props.searchUid}
                                  keyword={this.props.keyword}
                                  entityType={this.state.entityType}
                                  view={this.state.view}
                                  facets={this.state.facets}
                                  onTypeChange={this.onTypeChange}
                                  facetsDict={this.state.facetsDict}
                                  exactMatching={this.state.exactMatching}/>
            </div>);
        }
        return <div className="row">
            <div className="col-md-12 text-center">
                <img className="img-responsive center-block" src={context + "/assets/images/ajaxLoading.gif"}
                     alt="Loading results"/>
                <h2><img src={context + "/assets/images/ajaxLoader.gif"}/>{getTranslation("bittewarten")}</h2>
            </div>
        </div>;
    }
});


//************** Begin Facets Components *******************

// inject/ passing data
var FacetList = React.createClass({
    onFacetSelection: function (facetName,propertyName, valueSelected) {
        this.props.onFacetSelection(facetName,propertyName, valueSelected)
    },
    onFacetRemoval: function (facetName,propertyName, valueSelected) {
        this.props.onFacetRemoval(facetName,propertyName, valueSelected)
    },
    loadFacetsFromServer: function (eType,selectedFacets, exact) {
        var searchUrl = context + "/engine/api/searches/" + this.props.searchUid + "/facets?entityType=" + eType + "&lang=" + window.localStorage.lang + "&exact=" + exact
        $.ajax({
            type: 'POST',
            url: searchUrl,
            dataType: 'json',
            cache: false,
            data:JSON.stringify(selectedFacets),
            contentType: 'application/json',
            success: function (response) {
                if(exact && JSON.stringify(response["@graph"]) == undefined){
                    alert("NO RESULTS WERE MATCHED.")
                }else{
                    this.setState({data: response["@graph"]})
                }
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    getInitialState: function () {
        return {data: null, exactMatching:false};
    },
    componentDidMount: function () {
        this.loadFacetsFromServer(this.props.entityType,this.props.orgFacetsDict, this.props.exactMatching);
    },
    componentWillReceiveProps: function (nextProps) {
        // see if it actually changed
        if (nextProps.entityType !== this.props.entityType || nextProps.exactMatching !== this.props.exactMatching) {
            this.loadFacetsFromServer(nextProps.entityType,nextProps.orgFacetsDict, nextProps.exactMatching);
        }
        else{
            this.loadFacetsFromServer(this.props.entityType,nextProps.orgFacetsDict, this.props.exactMatching);
        }
    },
    handleExactMatchingChange: function () {
        this.props.onExactMatchingChange();
    },
    facets2CSV: function () {
        var JSONData = JSON.stringify(this.state.data);
        var ReportTitle = "Current results in CSV format"
        var ShowLabel = true;

        //If JSONData is not an object then JSON.parse will parse the JSON string in an Object
        var arrData = typeof JSONData != 'object' ? JSON.parse(JSONData) : JSONData;

        var CSV = '';
        //Set Report title in first row or line

        //CSV += ReportTitle + '\r\n\n';

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
            if (this.state.selectedChecks === undefined || this.state.selectedChecks === null || this.state.selectedChecks.length == 0 || this.state.selectedChecks.indexOf(i) > -1) {
                var row = "";
                var value_number_pair;
                //2nd loop will extract each column and convert it in string comma-seprated
                for (var index in arrData[i]) {
                    if(index==="http://vocab.lidakra.de/fuhsen/hasFacet"){
                        if(Object.prototype.toString.call(arrData[i][index]) === '[object Array]'){
                            for (var j = 0; j < arrData[i][index].length; j++){
                                value_number_pair=arrData[i][index][j].split("^")
                                for (var k = 0; k < value_number_pair.length; k++){
                                    row += '"' + value_number_pair[k] + '",';
                                }
                            }
                        }else{
                            value_number_pair=arrData[i][index].split("^")
                            for (var k = 0; k < value_number_pair.length; k++){
                                row += '"' + value_number_pair[k] + '",';
                            }
                        }
                    }else{
                        row += '"' + arrData[i][index] + '",';
                    }
                }

                row.slice(0, row.length - 1);

                //add a line break after each row
                CSV += row + '\r\n';
            }
        }

        if (CSV == '') {
            alert("Invalid data");
            return;
        }

        //Generate a file name
        var fileName = "Fuhsen_Facets_";
        //this will remove the blank-spaces from the title and replace it with an underscore
        fileName += ReportTitle.replace(/ /g, "_");

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

        this.setState({
            resultsData: this.state.resultsData,
            selected: this.state.selected,
            loading: this.state.loading,
            underDev: false,
            view: this.state.view
        });
    },
    render: function () {
        if (this.state.data && this.state.data !== undefined) {
            var _searchUid = this.props.searchUid;
            var _entityType = this.props.entityType;
            var MItems = this.state.data.map(function (menuItems) {
                if(menuItems["http://vocab.lidakra.de/fuhsen/hasFacet"] !== undefined) {
                    return <FacetItems searchUid={_searchUid}
                                       entityType={_entityType}
                                       label={menuItems["http://vocab.lidakra.de/fuhsen#facetLabel"]}
                                       name={menuItems["http://vocab.lidakra.de/fuhsen#facetName"]}
                                       property={menuItems["http://vocab.lidakra.de/fuhsen#value"]}
                                       facets={menuItems["http://vocab.lidakra.de/fuhsen/hasFacet"]}
                                       count={menuItems["http://vocab.lidakra.de/fuhsen#count"]}
                                       onFacetSelection={this.onFacetSelection}
                                       onFacetRemoval={this.onFacetRemoval}
                                       currentTab={this.props.currentTab}
                                       selectedFacets ={this.props.orgFacetsDict}/>
                }
            }, this);
            var exact_text = "Exact matching."
            var exact_property = ""

            switch (this.props.currentTab) {
                case "person":
                    exact_property="Regarding the full name (blue title)."
                    break;
                case "organization":
                    exact_property="Regarding the full name (blue title)."
                    break;
                case "product":
                    exact_property="Regarding the product description (blue title)."
                    break;
                case "website":
                    exact_text = "Match results starting with."
                    exact_property="Regarding the web title."
                    break;
                case "document":
                    exact_property="Regarding the document title (blue title)."
                    break;
            }

            const tooltipStyle = { display: this.state.hover ? 'block' : 'none'}

            return (
                <div id="facetsDiv" className="col-md-3 facets-container hidden-phone">
                    <div className="facets-head">
                        <h3>{getTranslation("resultfilters")}
                            <span className="export-facets-btn">(<a href="#" title={getTranslation("export_facets")} onClick={this.facets2CSV} className="no-external-link-icon">{getTranslation("export")}</a>)</span>
                        </h3>
                        <ContextualHelp type="contextual-help help" message={getTranslation("facets_help")}/>
                    </div>
                    <div className="js facets-list bt bb">
                        {MItems}
                    </div>
                    <div>
                        <input
                            name="isGoing"
                            type="checkbox"
                            checked={this.props.exactMatching}
                            onChange={this.handleExactMatchingChange} />
                        {getTranslation("exact_match")}
                    </div>
                </div>
            )
        }
        return (
            <div className="col-md-3 facets-container hidden-phone">
                <div className="facets-head">
                    <h3>{getTranslation("resultfilters")}</h3>
                </div>
                <div className="js facets-list bt bb">
                </div>
            </div>
        )

    }
});

var FacetItems = React.createClass({
    getInitialState: function () {
        arr_ele = [];//fill elements of the sub menu in an array
        return {showTextBox: false, selected_facets: []};
    },
    onClick: function () {
        var propsName = this.props.name;//.replace(/\s/g, '');
        var propsName_key = arr_ele.indexOf(propsName);
        //Check if the menu item is shown
        // if Yes hide it, if No show it

        if (this.state.showTextBox) {
            //Check if the item is in the array: means you just now clicked it, then hide it by setting the state to false and remove it from the array
            //if not in the array: means it was hidden by showing other item:
            //      - then show it by using normal js
            //      - set the state to true
            //      - hide and remove all others
            if (propsName_key >= 0) {
                this.setState({showTextBox: false});
                arr_ele.splice(propsName_key, 1);
            }
            else {
                this.setState({showTextBox: true});
                arr_ele.push(propsName);
                document.getElementById(propsName).style.display = "inline";
                if (arr_ele[0] != propsName) {
                    document.getElementById(arr_ele[0]).style.display = "none";
                    arr_ele.splice(0, 1);
                }
            }
        }
        else {
            this.setState({showTextBox: true});
            if (propsName_key < 0) {
                arr_ele.push(propsName);
            }
            for (var i = 0; i < arr_ele.length - 1; i++) {
                if (arr_ele[i] != propsName) {
                    document.getElementById(arr_ele[i]).style.display = "none";
                    arr_ele.splice(arr_ele[i], 1);
                }
            }
        }
    },
    onFacetItemClick: function (eSelectedItem) {
        var _selectedFacets = this.state.selected_facets;
        var _index = _selectedFacets.indexOf(eSelectedItem);
        if (_index < 0) {
            _selectedFacets.push(eSelectedItem);
            this.setState({showTextBox: false, selected_facets: _selectedFacets});
        }

        this.props.onFacetSelection(this.props.name,this.props.property, eSelectedItem)
    },
    onFacetItemRemoveClick: function (eSelectedItem) {
        if (eSelectedItem != "all") {
            var _selectedFacets = this.state.selected_facets;
            var _index = _selectedFacets.indexOf(eSelectedItem);
            if (_index >= 0) {
                _selectedFacets.splice(_index, 1);
                this.setState({showTextBox: false, selected_facets: _selectedFacets});
            }
        } else {
            this.setState({showTextBox: false, selected_facets: []});
        }

        this.props.onFacetRemoval(this.props.name, this.props.property,eSelectedItem)

    },
    componentWillUpdate: function (nextProps, nextState) {
        if (this.props.currentTab != nextProps.currentTab) {
            this.onFacetItemRemoveClick("all")
        }
    },
    OnDocumentClick: function (e) {
        if ($("#facetsDiv").has(e.target).length == 0){
            var propsName = this.props.name;//.replace(/\s/g, '');
            var propsName_key = arr_ele.indexOf(propsName);
            if (this.state.showTextBox && propsName_key > -1) {
                this.setState({showTextBox: false});
                arr_ele.splice(propsName_key, 1);
            }
        }
    },
    componentDidMount: function () {
        document.addEventListener('click', this.OnDocumentClick);
    },
    componentWillUnmount: function () {
        document.removeEventListener('click', this.OnDocumentClick);
    },
    render: function () {
        var selItems = [];
        var _onFacetItemRemoveClick = this.onFacetItemRemoveClick;
        if (this.props.selectedFacets[this.props.property]) {
            this.props.selectedFacets[this.props.property].map(function (item) {
                selItems.push(<li data-fctvalue={item}>
                    <span title="male" className="facet-value">{item}</span>
                    <a title="Remove" className="facet-remove fr" onClick={_onFacetItemRemoveClick.bind(this, item)}>
                    </a>
                </li>);
            });
            return (
                <div className="facets-item bt bb bl br">
                    <a className="h3" >{this.props.label}</a>
                    <div id={"" + this.props.name + ""}>
                        { this.state.showTextBox ?
                            <FacetSubMenuItems searchUid={this.props.searchUid} entityType={this.props.entityType}
                                               facetName={this.props.name}
                                               property = {this.props.property}
                                               facetsValues = {this.props.facets}
                                               onFacetItemClick={this.onFacetItemClick}/> : null }
                    </div>
                    <div className="flyout-left-container">
                        <ul className="selected-items unstyled">
                            {selItems}
                        </ul>
                    </div>
                </div>
            );
        }
        return (
            <div className="facets-item bt bb bl br">
                <a className="h3" onClick={this.onClick}>{this.props.label}</a>
                <div id={"" + this.props.name + ""}>
                    { this.state.showTextBox ?
                        <FacetSubMenuItems searchUid={this.props.searchUid} entityType={this.props.entityType}
                                           facetName={this.props.name}
                                           property = {this.props.property}
                                           facetsValues = {this.props.facets}
                                           onFacetItemClick={this.onFacetItemClick}/> : null }
                </div>
            </div>
        );
    }
});

var FacetSubMenuItems = React.createClass({
    loadFacetsFromServer: function (eFacet) {
        var searchUrl = context + "/engine/api/searches/" + this.props.searchUid + "/facets/" + eFacet + "?entityType=" + this.props.entityType;
        $.ajax({
            url: searchUrl,
            dataType: 'json',
            cache: false,
            success: function (data) {
                var facetsValues = data["@graph"];
                if (facetsValues !== undefined) {
                    facetsValues.sort(function (a, b) {
                        var count_a = parseInt(a["http://vocab.lidakra.de/fuhsen#count"]);
                        var count_b = parseInt(b["http://vocab.lidakra.de/fuhsen#count"]);
                        if (isNaN(count_a) || isNaN(count_b))
                            return 0;
                        if (count_a < count_b)
                            return 1;
                        if (count_a > count_b)
                            return -1;
                        return 0;
                    });
                }
                this.setState({data: facetsValues});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    getInitialState: function () {
        return {data: null};
    },
    sortFacetsValues : function(facetsValues){
        if (facetsValues !== undefined) {
            facetsValues.sort(function (a, b) {
                var count_a = parseInt(a["http://vocab.lidakra.de/fuhsen#count"]);
                var count_b = parseInt(b["http://vocab.lidakra.de/fuhsen#count"]);
                if (isNaN(count_a) || isNaN(count_b))
                    return 0;
                if (count_a < count_b)
                    return 1;
                if (count_a > count_b)
                    return -1;
                return 0;
            });
        }
        return facetsValues;
    },
    splitFacetsValues : function(){
        var facetData = [];
        if(Array.isArray(this.props.facetsValues)){
            this.props.facetsValues.map(function (facetElement) {
                var res = facetElement.split("^");
                if (res.length == 2) {
                    facetData.push({
                        "http://vocab.lidakra.de/fuhsen#value": res[0],
                        "http://vocab.lidakra.de/fuhsen#count": res[1]
                    });
                }
            });
        }
        else{
            var facetElement = this.props.facetsValues;
            if(facetElement.length >0){
                var res = facetElement.split("^");
                if (res.length == 2) {
                    facetData.push({
                        "http://vocab.lidakra.de/fuhsen#value": res[0],
                        "http://vocab.lidakra.de/fuhsen#count": res[1]
                    });
                }
            }
        }
        return facetData;
    },
    componentDidMount: function () {
        //this.loadFacetsFromServer(this.props.facetName);
        var facetsValues = this.splitFacetsValues();
        facetsValues = this.sortFacetsValues(facetsValues);
        this.setState({data: facetsValues});
    },
    render: function () {
        var subMenuEle = [];
        if (this.state.data && this.state.data !== undefined) {
            var _onFacetItemClick = this.props.onFacetItemClick;
            this.state.data.map(function (menuItems) {
                if (menuItems["http://vocab.lidakra.de/fuhsen#value"] && menuItems["http://vocab.lidakra.de/fuhsen#value"] !== "blank") {
                    subMenuEle.push(<li ><a href="#" id={menuItems["http://vocab.lidakra.de/fuhsen#value"]}
                                            onClick={_onFacetItemClick.bind(this, menuItems["http://vocab.lidakra.de/fuhsen#value"])}>
                       <span
                           className="sub-item-result">({menuItems["http://vocab.lidakra.de/fuhsen#count"]})</span>
                        {menuItems["http://vocab.lidakra.de/fuhsen#value"]}</a>
                    </li>);
                }
            });
        }
        return (
            <div>
                <div className="flyout-left-container">
                    <ul className="selected-items unstyled"></ul>
                    <div className="input-search-fct-container">
                        <input type="text" className="input-search-fct"/>
                    </div>
                </div>

                <div className="flyout-right-container">
                    <div className="flyout-right-head">
                        <span>{getTranslation("sortedby")}</span>
                        <div className="flyout-page-nav fr">
                            <ul className="inline">
                                <li className="pages-overall-index">{getTranslation("page")}<span>1</span></li>
                            </ul>
                        </div>
                    </div>
                    <div className="flyout-right-body">
                        <ul className="left-col unstyled">
                            {subMenuEle}
                        </ul>
                    </div>
                </div>
            </div>
        );
    }
});

//************** End Facets Components *******************
var ResultsContainer = React.createClass({
    checksListener: function (listOfSelectedRows) {
        this.setState({selectedChecks: listOfSelectedRows})
    },
    noData: function () {
        alert(getTranslation("nodata"));
    },
    toggleResultsView: function () {
        var view_selector = (this.state.view == "list" ? "table" : "list");
        this.setState({
            resultsData: this.state.resultsData,
            selected: this.state.selected,
            loading: this.state.loading,
            underDev: false,
            view: view_selector,
            selectedChecks: []
        });
    },
    underDevelopmentFunction: function () {
        this.setState({
            resultsData: this.state.resultsData,
            selected: this.state.selected,
            loading: this.state.loading,
            underDev: true,
            view: this.state.view
        });
    },
    crawlAll: function () {
        var JSONData = this.state.resultsData;

        var seeds = []

        for (var key in JSONData) {
            seeds.push(JSONData[key].url)
        }

        var createCrawlJobUrl = context + "/crawling/jobs/create";

        $.ajax({
            url: createCrawlJobUrl,
            data: JSON.stringify({"seedURLs": seeds}),
            type: "POST",
            dataType: "text",
            contentType: "application/json; charset=utf-8",
            cache: false,
            success: function () {
                this.setState({crawled: true});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });

    },
    csvFunction: function (e) {
        var JSONData = JSON.stringify(this.state.resultsData);
        var ReportTitle = "Current results in CSV format"
        var ShowLabel = true;

        //If JSONData is not an object then JSON.parse will parse the JSON string in an Object
        var arrData = typeof JSONData != 'object' ? JSON.parse(JSONData) : JSONData;

        var CSV = '';
        //Set Report title in first row or line

        //CSV += ReportTitle + '\r\n\n';

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
            if (this.state.selectedChecks === undefined || this.state.selectedChecks === null || this.state.selectedChecks.length == 0 || this.state.selectedChecks.indexOf(i) > -1) {
                var row = "";

                //2nd loop will extract each column and convert it in string comma-seprated
                for (var index in arrData[i]) {
                    row += '"' + arrData[i][index] + '",';
                }

                row.slice(0, row.length - 1);

                //add a line break after each row
                CSV += row + '\r\n';
            }
        }

        if (CSV == '') {
            alert("Invalid data");
            return;
        }

        //Generate a file name
        var fileName = "Fuhsen_";
        //this will remove the blank-spaces from the title and replace it with an underscore
        fileName += ReportTitle.replace(/ /g, "_");

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

        this.setState({
            resultsData: this.state.resultsData,
            selected: this.state.selected,
            loading: this.state.loading,
            underDev: false,
            view: this.state.view
        });
    },
    loadDataFromServer: function (eType, exactMatching) {
        this.setState({selected: eType, loading: true});
        var searchUrl = context + "/engine/api/searches/" + this.props.searchUid + "/results?entityType=" + eType + "&sources=" + sourcesDirty + "&types=" + typesDirty + "&exact=" + exactMatching
        $.ajax({
            url: searchUrl,
            dataType: 'json',
            cache: false,
            success: function (data) {
                if(exactMatching && JSON.stringify(data["@graph"]) == undefined){
                    this.setState({
                        resultsData: this.state.resultsData,
                        selected: eType,
                        loading: false,
                        underDev: false,
                        originalData: this.state.originalData
                    })
                }else{
                    if(Object.keys(this.state.results_stat).length == 0)
                        this.computeDataStatistics();
                    data_to_handle = JSON.parse(JSON.stringify(data));
                    //alert(JSON.stringify(data_to_handle));
                    if (data_to_handle["@graph"] !== undefined)
                        data_to_handle = data_to_handle["@graph"].sort(compareRank);
                    else {
                        if (data_to_handle["fs:source"] !== undefined) {
                            data_to_handle = JSON.parse("{ \"@graph\": [" + JSON.stringify(data) + "]}");
                            data_to_handle = data_to_handle["@graph"].sort(compareRank);
                        }
                        else
                            data_to_handle = undefined;
                    }

                    data_to_maintain = data_to_handle;
                    //alert(JSON.stringify(data_to_handle));
                    //data_to_handle = JSON.parse(JSON.stringify(data))
                    //data_to_maintain = JSON.parse(JSON.stringify(data))
                    this.setState({
                        resultsData: data_to_handle,
                        selected: eType,
                        loading: false,
                        underDev: false,
                        originalData: data_to_maintain
                    });
                }
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    computeDataStatistics: function(){
        var stat_url = context + "/engine/api/searches/" + this.props.searchUid + "/results_stat";
        var moreResultsHelper = false;
      $.ajax({
          url: stat_url,
          dataType: 'json',
          cache: true,
          success: function (data) {
              var stat = {};
              stat["person"] = stat["organization"] = stat["product"] = stat["website"] = stat["document"] = 0;
              if (data["@graph"] === undefined && data["http://vocab.lidakra.de/fuhsen#value"] !== undefined)
                  data = JSON.parse("{ \"@graph\": [" + JSON.stringify(data) + "]}");

              if(data["@graph"] !== undefined) {
                  data["@graph"].map(function (result) {
                      if (result["http://vocab.lidakra.de/fuhsen#nextPage"] !== undefined)
                          moreResultsHelper = true;
                      else
                        stat[result["http://vocab.lidakra.de/fuhsen#value"]] = result["http://vocab.lidakra.de/fuhsen#count"];
                  });
              }

              this.setState({
                    results_stat: stat,
                    areThereMoreResults: moreResultsHelper
                });

          }.bind(this),
          error: function (xhr, status, err) {
              console.error(this.props.url, status, err.toString());
          }.bind(this)
      });
    },
    getInitialState: function () {
        return {
            resultsData: "",
            selected: "person",
            loading: true,
            underDev: false,
            crawled: false,
            view: this.props.view,
            selectedChecks: [],
            results_stat: {},
            areThereMoreResults: false
        };
    },
    componentDidMount: function () {
        this.loadDataFromServer(this.props.entityType, this.props.exactMatching);
    },
    componentWillReceiveProps: function (nextProps) {
        // see if it actually changed
        if (nextProps.entityType !== this.props.entityType || nextProps.exactMatching !== this.props.exactMatching) {
            this.loadDataFromServer(nextProps.entityType, nextProps.exactMatching);
        }
    },
    render: function () {

        var loadMoreResultsItem = <div id="load-more-results" className="hidden">Load More Results</div>
        if (this.state.areThereMoreResults) {
            loadMoreResultsItem = <a href="mailto:lidakra-support@@ontos.com"><div id="load-more-results">Load More Results</div></a>
        }

        var personenItem = <li className="headers-li" onClick={this.props.onTypeChange}
                               data-id="1">{getTranslation("people")+'(' + this.state.results_stat["person"]+ ')'}</li>
        var organizationenItem = <li className="headers-li" onClick={this.props.onTypeChange}
                                     data-id="2">{getTranslation("organisations")+'(' + this.state.results_stat["organization"]+ ')'}</li>
        var produkteItem = <li className="headers-li" onClick={this.props.onTypeChange}
                               data-id="3">{getTranslation("products")+'(' + this.state.results_stat["product"]+ ')'}</li>
        var darkWebItem = <li className="headers-li" onClick={this.props.onTypeChange}
                              data-id="4">{getTranslation("tor_websites")+'(' + this.state.results_stat["website"]+ ')'}</li>
        var documentItem = <li className="headers-li" onClick={this.props.onTypeChange}
                               data-id="5">{getTranslation("documents")+'(' + this.state.results_stat["document"]+ ')'}</li>

        if (this.state.selected === "person") {
            personenItem = <li className="headers-li" onClick={this.props.onTypeChange} data-id="1"><p>
                <b>{getTranslation("people")+'(' + this.state.results_stat[this.state.selected]+ ')'}</b></p></li>
        } else if (this.state.selected === "organization") {
            organizationenItem = <li className="headers-li" onClick={this.props.onTypeChange} data-id="2"><p>
                <b>{getTranslation("organisations")+'(' + this.state.results_stat[this.state.selected]+ ')'}</b></p></li>
        } else if (this.state.selected === "product") {
            produkteItem = <li className="headers-li" onClick={this.props.onTypeChange} data-id="3"><p>
                <b>{getTranslation("products")+'(' +  this.state.results_stat[this.state.selected]+ ')'}</b></p></li>
        } else if (this.state.selected === "website") {
            darkWebItem = <li className="headers-li" onClick={this.props.onTypeChange} data-id="4"><p>
                <b>{getTranslation("tor_websites")+'(' + this.state.results_stat[this.state.selected]+ ')'}</b></p></li>
        } else if (this.state.selected === "document") {
            documentItem = <li className="headers-li" onClick={this.props.onTypeChange} data-id="5"><p>
                <b>{getTranslation("documents")+'(' + this.state.results_stat[this.state.selected]+ ')'}</b></p></li>
        }

        if (this.state.loading) {
            return <div className="col-md-9">
                <div id="results-paginator-options" className="results-paginator-options">
                    <div class="off result-pages-count"></div>
                    <div className="row">
                        <div className="col-md-8 tabulator">
                            <ul className="list-inline">
                                {/*<li>*/}
                                    {/*<span className="total-results-label"> {getTranslation("results")}:</span>*/}
                                {/*</li>*/}
                                {personenItem}
                                {organizationenItem}
                                {produkteItem}
                                {darkWebItem}
                                {documentItem}
                            </ul>
                        </div>
                    </div>
                </div>

                <div className="row">
                    <div className="col-md-12 text-center">
                        <img className="img-responsive center-block" src={context + "/assets/images/ajaxLoading.gif"}
                             alt="Loading results"/>
                        <h2><img src={context + "/assets/images/ajaxLoader.gif"}/>{getTranslation("bittewarten")}</h2>
                    </div>
                </div>
            </div>;
        }

        var final_data = this.state.resultsData;

        //Facets filtering algorithm
        if (Object.keys(this.props.facetsDict).length > 0) {
            final_data = this.state.originalData
            for (var key in this.props.facetsDict) {
                if (this.props.facetsDict.hasOwnProperty(key)) {
                    var facet_name = "fs:" + key
                    var facet_values = this.props.facetsDict[key]

                    function containsAll(source, target) {
                        var found = false;
                        for (var i = 0; i < target.length; i++) {
                            if (source.indexOf(target[i]) > -1) {
                                found = true;
                                break;
                            }
                        }
                        return found;
                    }

                    function filterByFacet(obj) {
                        //Comparing array of elements
                        if (Array.isArray(obj[facet_name])) {
                            if (containsAll(obj[facet_name], facet_values))
                                return true;
                            else
                                return false;
                        }
                        //Comparing just one element
                        else {
                            if (facet_values.indexOf(obj[facet_name]) >= 0)
                                return true;
                            else
                                return false;
                        }
                    }

                    var as = final_data.filter(filterByFacet);
                    final_data = JSON.parse(JSON.stringify(as));
                }
            }
        }
        else {
            final_data = this.state.originalData;
        }
        //No results
        if (final_data === undefined) {
            return <div className="col-md-9">
                <div id="results-paginator-options" className="results-paginator-options">
                    <div class="off result-pages-count"></div>
                    <div className="row">
                        <div className="col-md-8 tabulator">
                            <ul className="list-inline">
                                {/*<li>*/}
                                    {/*<span className="total-results">0</span>*/}
                                    {/*<span className="total-results-label"> {getTranslation("results")}:</span>*/}
                                {/*</li>*/}
                                {personenItem}
                                {organizationenItem}
                                {produkteItem}
                                {darkWebItem}
                                {documentItem}
                            </ul>
                        </div>
                        <div className="col-md-4 text-right">
                            &nbsp;
                        </div>
                    </div>
                </div>
                <div className="search-results-content">
                    <div className="row">
                        <div className="col-md-12">
                            <ul id="search-results" className="search-results">
                                <ul className="results-list list-unstyled">
                                    <h1>{getTranslation("no_results")}</h1>
                                </ul>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        }

        var stat_text = (final_data.length <  this.state.results_stat[this.state.selected] ?  final_data.length + '/' : "")+ this.state.results_stat[this.state.selected];
        if (this.state.selected === "person") {
            personenItem = <li className="headers-li" onClick={this.props.onTypeChange} data-id="1"><p>
                <b>{getTranslation("people")+'(' + stat_text + ')'}</b></p></li>
        } else if (this.state.selected === "organization") {
            organizationenItem = <li className="headers-li" onClick={this.props.onTypeChange} data-id="2"><p>
                <b>{getTranslation("organisations")+'(' + stat_text+ ')'}</b></p></li>
        } else if (this.state.selected === "product") {
            produkteItem = <li className="headers-li" onClick={this.props.onTypeChange} data-id="3"><p>
                <b>{getTranslation("products")+'(' + stat_text + ')'}</b></p></li>
        } else if (this.state.selected === "website") {
            darkWebItem = <li className="headers-li" onClick={this.props.onTypeChange} data-id="4"><p>
                <b>{getTranslation("tor_websites")+'(' + stat_text + ')'}</b></p></li>
        } else if (this.state.selected === "document") {
            documentItem = <li className="headers-li" onClick={this.props.onTypeChange} data-id="5"><p>
                <b>{getTranslation("documents")+'(' + stat_text + ')'}</b></p></li>
        }

        if (this.state.underDev) {
            return <div className="col-md-9">
                <div id="results-paginator-options" className="results-paginator-options">
                    <div class="off result-pages-count"></div>
                    <div className="row">
                        <div className="col-md-8 tabulator">
                            <ul className="list-inline">
                                {/*<li>*/}
                                    {/*<span className="total-results">{final_data.length}</span>*/}
                                    {/*<span className="total-results-label"> {getTranslation("results")}:</span>*/}
                                {/*</li>*/}
                                {personenItem}
                                {organizationenItem}
                                {produkteItem}
                                {darkWebItem}
                                {documentItem}
                            </ul>
                        </div>
                        <div className="col-md-4 text-right">
                            &nbsp;
                        </div>
                    </div>
                </div>
                <div className="search-results-content">
                    <div className="row">
                        <div className="col-md-12">
                            <ul id="search-results" className="search-results">
                                <ul className="results-list list-unstyled">
                                    <h1>{getTranslation("underdevelopment")}</h1>
                                </ul>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        }

        return <div className="col-md-9">
            <div id="results-paginator-options" className="results-paginator-options">
                <div class="off result-pages-count"></div>
                <div className="row">
                    <div className="col-md-8 tabulator">
                        <div className="tabs-head">
                            <ul className="list-inline">
                                {/*<li>*/}
                                    {/*<span className="total-results">{final_data.length}</span>*/}
                                    {/*<span className="total-results-label"> {getTranslation("results")}:</span>*/}
                                {/*</li>*/}
                                {personenItem}
                                {organizationenItem}
                                {produkteItem}
                                {darkWebItem}
                                {documentItem}
                            </ul>
                            <SearchMetadataInfo searchUid={this.props.searchUid}/>
                        </div>
                    </div>
                    <div className="col-md-4 text-right">
                        {/*{ this.state.selected === "website" ? <CustomForm id="btn_crawl" class_identifier="crawl_icon"*/}
                                                                          {/*func={this.crawlAll}/> : null }*/}
                        {/*{ this.state.selected === "website" ? <div className="divider"/> : null }*/}
                        <CustomForm id="btn_view_selector"
    class_identifier={(this.state.view == "list" ? "table" : "list") + "_icon"}
    func={this.toggleResultsView}/>
                        <div className="divider"/>
                        <CustomForm id="btn_csv" class_identifier="csv_icon" func={this.csvFunction}/>
                    </div>
                </div>
            </div>
            <div className="search-results-content">
                <div className="row">
                    { this.state.view == "list" ?
                        <div className="col-md-12">
                            <ul id="search-results" className="search-results">
                                <ul className="results-list list-unstyled">
                                    <ResultsList data={final_data}
                                                 crawled={this.state.crawled}>
                                    </ResultsList>

                                </ul>
                            </ul>
                        </div>
                        :
                        <div id="search-results" className="search-results">
                            <ResultsTable data={final_data}
                                          crawled={this.state.crawled}
                                          type={this.props.entityType}
                                          checksListener={this.checksListener}
                            >
                            </ResultsTable>
                        </div>
                    }
                </div>
            </div>
            {loadMoreResultsItem}
        </div>
    }
});

var CustomForm = React.createClass({
    render: function () {
        return (
            <button id={this.props.id} onClick={this.props.func} className={this.props.class_identifier}
                    title={getTranslation(this.props.class_identifier)}>
            </button>
        );
    }
});

var ResultsList = React.createClass({
    render: function () {

        var resultsNodesSorted = this.props.data;

        var already_crawled = this.props.crawled;
        var resultsNodes = resultsNodesSorted.map(function (result,idx) {
            if (result["@type"] === "foaf:Person") {
                return (
                    <PersonResultElement
                        id = {idx}
                        img={result.image}
                        name={result["fs:title"]}
                        source={result["fs:source"]}
                        alias={result["fs:alias"]}
                        location={result["fs:location"]}
                        label={result["fs:label"]}
                        comment={result["fs:comment"]}
                        gender={result["fs:gender"]}
                        occupation={result["fs:occupation"]}
                        birthday={result["fs:birthday"]}
                        country={result["fs:country"]}
                        webpage={result.url}
                        active_email={result["fs:active_email"]}
                        wants={result["fs:wants"]}
                        haves={result["fs:haves"]}
                        top_haves={result["fs:top_haves"]}
                        interests={result["fs:interests"]}
                        jsonResult = {result}
                    >
                    </PersonResultElement>
                );
            } else if (result["@type"] === "foaf:Organization") {
                return (
                    <OrganizationResultElement
                        id = {idx}
                        img={result.image}
                        title={result["fs:title"]}
                        source={result["fs:source"]}
                        label={result["fs:label"]}
                        comment={result["fs:comment"]}
                        country={result["fs:country"]}
                        location={result["fs:location"]}
                        webpage={result.url}
                        jsonResult = {result}>
                    </OrganizationResultElement>
                );
            } else if (result["@type"] === "gr:ProductOrService") {
                return (
                    <ProductResultElement
                        id = {idx}
                        img={result.image}
                        title={result["fs:title"]}
                        source={result["fs:source"]}
                        location={result["fs:location"]}
                        country={result["fs:country"]}
                        price={result["fs:priceLabel"]}
                        condition={result["fs:condition"]}
                        webpage={result.url}
                        jsonResult = {result}>
                    </ProductResultElement>
                );
            } else if (result["@type"] === "foaf:Document") {
                if (result["fs:source"] === "ELASTIC") {
                    return (
                        <ElasticSearchResultElement
                            img={context + "/assets/images/datasources/Elasticsearch.png"}
                            content={result["fs:content"]}
                            label={result["fs:title"]}
                            onion_url={result.url}
                            entity_url={result["fs:entity_url"]}
                            entity_dbpedia={result["fs:entity_dbpedia"]}
                            entity_type={result["fs:entity_type"]}
                            entity_name={result["fs:entity_name"]}>
                        </ElasticSearchResultElement>
                    );
                } else {
                    return (
                        <WebResultElement
                            img={context + "/assets/images/datasources/TorLogo.png"}
                            onion_url={result["url"]}
                            comment={result["fs:comment"]}
                            source={result["fs:source"]}
                            onion_label={result["rdfs:label"]}
                            crawled={already_crawled}>
                        </WebResultElement>
                    );
                }
            } else if (result["@type"] === "fs:Document") {
                return (
                    <DocumentResultElement
                        label={result["fs:label"]}
                        comment={result["fs:comment"]}
                        webpage={result.url}
                        country={result["fs:country"]}
                        language={result["fs:language"]}
                        filename={result["fs:file_name"]}
                        extension={result["fs:extension"]}
                        source={result["fs:source"]}>
                    </DocumentResultElement>
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

var WebResultElement = React.createClass({
    checkOnionSite: function () {
        var searchUrl = context + "/checkOnionSite?site=" + this.props.onion_url;

        $.ajax({
            url: searchUrl,
            dataType: 'json',
            cache: false,
            success: function (data) {
                if(data.valid) {
                    this.createCrawlJob();
                }
                else {
                    alert(getTranslation("tor_invalid_websites"));
                    this.setState({validTORSite: false});
                }

            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    createCrawlJob: function () {
        console.info("Creating crawl job task")
        var createCrawlJobUrl = context + "/crawling/jobs/create";

        $.ajax({
            url: createCrawlJobUrl,
            data: JSON.stringify({"seedURLs": [this.props.onion_url]}),
            type: "POST",
            dataType: "text",
            contentType: "application/json; charset=utf-8",
            cache: false,
            success: function (response) {
                var idx = response.lastIndexOf('/');
                var crawlID = null;
                if(idx !== -1)
                    crawlID = response.substr(idx+1);
                var timer = setInterval(this.getCrawlJobStatus,5000);
                this.setState({crawlJobCreated: true,crawlID: crawlID,jobStatus: "crawlJobCreated",timerJobStatus : timer });
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    getCrawlJobStatus: function(){
      var jobStatusUrl = context + "/crawling/jobs/" + this.state.crawlID + "/status";
        $.ajax({
            url: jobStatusUrl,
            contentType: "application/json; charset=utf-8",
            cache: false,
            success: function (response) {
                var status = response['crawlStatus'];
                if(status === "FINISHED" || status === "FAILED")
                    clearInterval(this.state.timerJobStatus);
                this.setState({jobStatus: "crawlJob"+ status});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    onCreateCrawlJobClick: function () {
        this.checkOnionSite();
        this.setState({jobStatus:"validatingUrl"});
    },
    getInitialState: function () {
        return {crawlJobCreated: false, validTORSite: true,crawlID: null,jobStatus: null,timerJobStatus: null };
    },
    onClickLink : function(url,e){
        e.preventDefault();
        if(navigator.appCodeName == "Mozilla") //"Mozilla" is the application code name for both Chrome, Firefox, IE, Safari, and Opera.
            url = url.replace(".onion",".onion.to");
        window.open(url,'_blank');
    },
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
                                <p><b>Web title</b>: {this.props.onion_label}</p>
                                <p><b>{getTranslation("comment")}</b>: {this.props.comment}</p>
                                <p><b>Link: </b><a href={this.props.onion_url}
                                                     onClick={this.onClickLink.bind(this,this.props.onion_url)}>{getTranslation("clickhere")}</a></p>
                            </div>
                        </div>
                    </div>
                    <div>
                        <div>
                            <div>
                                <img src={context + "/assets/images/datasources/" + this.props.source + ".png"}
                                     alt={"Information from " + this.props.source} height="45" width="45"
                                     title={this.props.source}/>
                            </div>
                            <div>
                                {this.state.validTORSite ? this.props.crawled == true || this.state.crawlJobCreated === true || this.state.jobStatus !== null ?
                                <label>{this.state.jobStatus !== "crawlJobFINISHED" && this.state.jobStatus !== "crawlJobFAILED" ? <img src={context+"/assets/images/ajaxLoader.gif"}/> : null }{getTranslation(this.state.jobStatus)}</label> : <button
                                onClick={this.onCreateCrawlJobClick}>&nbsp;{getTranslation("createCrawlJob")}&nbsp;</button> : getTranslation("invalid_website") }
                            </div>
                        </div>
                    </div>
                </div>
            </li>
        );
    }
});

var SnapshotLink = React.createClass({
    showPDF: function () {
        var url = context + "/screenshot?url=" + this.props.webpage;
        window.open(url);
    },
    render: function () {
        return (
            <a href="#" onClick={this.showPDF}> --- (See Snapshot!)</a>
        );
    }
});

var ProductResultElement = React.createClass({
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
                                {this.props.title}
                            </h2>
                            <div className="subtitle">
                                { this.props.location !== undefined ?
                                    <p>{getTranslation("location")}: {this.props.location}</p> : null }
                                { this.props.country !== undefined ?
                                    <p>{getTranslation("country")}: {this.props.country}</p> : null }
                                { this.props.price !== undefined ?
                                    <p>{getTranslation("price")}: {this.props.price}</p> : null }
                                { this.props.condition !== undefined ?
                                    <p>{getTranslation("condition")}: {this.props.condition}</p> : null }
                                { this.props.webpage !== undefined ?
                                    <p><b>{getTranslation("link")}: </b><a href={this.props.webpage}
                                                                           target="_blank">{this.props.webpage}</a><SnapshotLink
                                        webpage={this.props.webpage}></SnapshotLink></p> : null }
                            </div>
                        </div>
                    </div>
                    <div class="thumbnail-wrapper col-md-1">
                        <div class="thumbnail">
                            <img src={context + "/assets/images/datasources/" + this.props.source + ".png"}
                                 alt={"Information from " + this.props.source} height="45" width="45"
                                 title={this.props.source}/>
                        </div>
                        {/*<Graph id={"graph"+this.props.id} entity={this.props.jsonResult}/>*/}
                    </div>
                </div>
            </li>
        );
    }
});

var PersonResultElement = React.createClass({
    render: function () {
        return (
            <li className="item bt">
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
                            <h2 className="title">
                                {this.props.name}
                            </h2>
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
                                    <p><b>{getTranslation("link")}: </b><a href={this.props.webpage}
                                                                           target="_blank">{this.props.webpage}</a>
                                        <SnapshotLink webpage={this.props.webpage}></SnapshotLink></p> : null }
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
                        <div class="thumbnail">
                            <img src={context + "/assets/images/datasources/" + this.props.source + ".png"}
                                 alt={"Information from " + this.props.source} height="45" width="45"
                                 title={this.props.source}/>
                        </div>
                        {/*<Graph id={"graph"+this.props.id} entity={this.props.jsonResult}/>*/}
                    </div>
                </div>
            </li>
        );
    }
});

var OrganizationResultElement = React.createClass({
    render: function () {
        return (
            <li className="item bt">
                <div className="summary row">
                    <div className="thumbnail-wrapper col-md-2">
                        <div className="thumbnail">
                            { this.props.img !== undefined ? <img src={this.props.img} height="60px" width="75px"/>:
                                <img src={context + "/assets/images/datasources/Unknown_Thing.jpg"} height="60px" width="75px"/> }
                        </div>
                    </div>
                    <div className="summary-main-wrapper col-md-8">
                        <div className="summary-main">
                            <h2 className="title">
                                {this.props.title}
                            </h2>
                            <div className="subtitle">
                                { this.props.label !== undefined ? <p>{this.props.label}</p> : null }
                                { this.props.comment !== undefined ? <p>{this.props.comment}</p> : null }
                                { this.props.country !== undefined ?
                                    <p>{getTranslation("country")}: {this.props.country}</p> : null }
                                { this.props.location !== undefined ?
                                    <p>{getTranslation("location")}: {this.props.location}</p> : null }
                                { this.props.webpage !== undefined ?
                                    <p><b>{getTranslation("link")}: </b><a href={this.props.webpage}
                                                                           target="_blank">{this.props.webpage}</a><SnapshotLink
                                        webpage={this.props.webpage}></SnapshotLink></p> : null }
                            </div>
                        </div>
                    </div>
                    <div class="thumbnail-wrapper col-md-1">
                        <div class="thumbnail">
                            <img src={context + "/assets/images/datasources/" + this.props.source + ".png"}
                                 alt={"Information from " + this.props.source} height="45" width="45"
                                 title={this.props.source}/>
                        </div>
                        {/*<Graph id={"graph"+this.props.id} entity={this.props.jsonResult}/>*/}
                    </div>
                </div>
            </li>
        );
    }
});

var ElasticSearchResultElement = React.createClass({
    onClickLink : function(url,e){
        e.preventDefault();
        if(navigator.appCodeName == "Mozilla") //"Mozilla" is the application code name for both Chrome, Firefox, IE, Safari, and Opera.
            url = url.replace(".onion",".onion.to");
        window.open(url,'_blank');
    },
    render: function () {
        return (
            <li className="item bt">
                <div className="summary row">
                    <div className="thumbnail-wrapper col-md-2">
                        <div className="thumbnail">
                            { this.props.img !== undefined ? <img src={this.props.img} height="60px" width="75px"/>:
                                <img src={context + "/assets/images/datasources/Unknown_Thing.jpg"} height="60px" width="75px"/> }
                        </div>
                    </div>
                    <div className="summary-main-wrapper col-md-8">
                        <div className="summary-main">
                            <h2 className="title">
                                {this.props.label}
                            </h2>
                            <div className="subtitle">
                                { this.props.onion_url !== undefined ?
                                    <p><b>{getTranslation("link")}: </b><a href={this.props.onion_url}
                                                                           onClick={this.onClickLink.bind(this,this.props.onion_url)}>{this.props.onion_url}</a><SnapshotLink
                                        webpage={this.props.onion_url.replace(".onion",".onion.to")}></SnapshotLink></p> : null }
                                { this.props.content !== undefined ?
                                    <p><b>Content: </b>{<RichText label="Content" text={this.props.content} maxLength={300}/>}</p> : null }
                                { this.props.entity_url !== undefined ?
                                    <p><b>Entity URL: </b>{this.props.entity_url}</p> : null }
                                { this.props.entity_dbpedia !== undefined ?
                                    <p><b>Entity DBPedia: </b>{this.props.entity_dbpedia}</p> : null }
                                { this.props.entity_type !== undefined ?
                                    <p><b>Entity type: </b>{this.props.entity_type}</p> : null }
                                { this.props.entity_name !== undefined ?
                                    <p><b>Entity name: </b>{this.props.entity_name}</p> : null }
                            </div>
                        </div>
                    </div>
                    <div class="thumbnail-wrapper col-md-1">
                        <div class="thumbnail">
                            <img src={context + "/assets/images/datasources/Elasticsearch.png"}
                                 alt={"Information from " + this.props.source} height="45" width="45"
                                 title="Elasticsearch"/>
                        </div>
                    </div>
                </div>
            </li>
        );
    }
});

var DocumentResultElement = React.createClass({
    render: function () {
        return (
            <li className="item bt">
                <div className="summary row">
                    <div className="thumbnail-wrapper col-md-2">
                        <div className="thumbnail">
                            { this.props.extension !== undefined ? <img src={context + "/assets/images/icons/" + this.props.extension + ".png"} height="60px" width="75px"/>:
                                <img src={context + "/assets/images/datasources/Unknown_Thing.jpg"} height="60px" width="75px"/> }
                        </div>
                    </div>
                    <div className="summary-main-wrapper col-md-8">
                        <div className="summary-main">
                            <h2 className="title">
                                {this.props.label}
                            </h2>
                            <div className="subtitle">
                                { this.props.comment !== undefined ? <p>{this.props.comment}</p> : null }
                                { this.props.country !== undefined ?
                                    <p>{getTranslation("country")}: {this.props.country}</p> : null }
                                { this.props.language !== undefined ?
                                    <p>{getTranslation("language")}: {this.props.language}</p> : null }
                                { this.props.filename !== undefined ?
                                    <p>{getTranslation("filename")}: {this.props.filename}</p> : null }
                                { this.props.webpage !== undefined ?
                                    <p><b>{getTranslation("link")}: </b><a href={this.props.webpage}
                                                                           target="_blank">{this.props.webpage}</a><SnapshotLink
                                        webpage={this.props.webpage}></SnapshotLink></p> : null }
                            </div>
                        </div>
                    </div>
                    <div class="thumbnail-wrapper col-md-1">
                        <div class="thumbnail">
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

var ContextualHelp = React.createClass({
    onChange: function() {
        if(this.state.showSourcesTypesDiv) {
            this.setState({ showSourcesTypesDiv: false});
        } else {
            this.setState({ showSourcesTypesDiv: true});
        }
    },
    getInitialState: function() {
        return { showSourcesTypesDiv: false };
    },
    render: function () {
        var floatingDivStyle = this.state.showSourcesTypesDiv ? "popuptext popupshow" : "popuptext"
        return (
            <div className={this.props.type} onClick={this.onChange}>
                <span className={floatingDivStyle}>
                    {this.props.message}
                </span>
            </div>
        );
    }
});

var SearchMetadataInfo = React.createClass({
    loadSearchMetadata: function () {
        var searchUrl = context + "/engine/api/searches/" + this.props.searchUid + "/metadata";
        var messageBuilder = "";
        if (!this.state.isDataLoad) {
            $.ajax({
                url: searchUrl,
                dataType: 'json',
                cache: false,
                success: function (data) {
                    if (data["@graph"] === undefined && data["fs:wrapperLabel"] !== undefined)
                        data = JSON.parse("{ \"@graph\": [" + JSON.stringify(data) + "]}");

                    if(data["@graph"] !== undefined) {
                        data["@graph"].map(function (result) {
                            if (result["rdfs:label"] === "200") {
                                messageBuilder = messageBuilder + result["fs:wrapperLabel"] + getTranslation("search_ok");
                            }
                            else {
                                messageBuilder = messageBuilder + result["fs:wrapperLabel"] + getTranslation("search_error");
                            }
                        });
                    }

                    messageBuilder = messageBuilder.replace("ebay", "eBay");
                    messageBuilder = messageBuilder.replace("elasticsearch", "Crawled Onion websites");
                    messageBuilder = messageBuilder.replace("facebook", "Facebook");
                    messageBuilder = messageBuilder.replace("gkb", "Google Knowledge Graph");
                    messageBuilder = messageBuilder.replace("gplus", "Google+");
                    messageBuilder = messageBuilder.replace("linkedleaks", "Linked Leaks");
                    messageBuilder = messageBuilder.replace("occrp", "OCCRP");
                    messageBuilder = messageBuilder.replace("tor2web", "Onion websites");
                    messageBuilder = messageBuilder.replace("twitter", "Twitter");
                    messageBuilder = messageBuilder.replace("xing", "Xing");
                    messageBuilder = messageBuilder.replace(",", ", ");

                    this.setState({isDataLoad: true, message: messageBuilder});

                }.bind(this)
            });
        }
    },
    componentDidMount: function () {
        this.loadSearchMetadata();
    },
    getInitialState: function() {
        return { isDataLoad: false, message: "" };
    },
    render: function () {
        return (
            <ContextualHelp type="contextual-help info" message={this.state.message}/>
        );
    }
});

React.render(<ContainerResults url={context + "/keyword"} pollInterval={200000}/>, document.getElementById('skeleton'));