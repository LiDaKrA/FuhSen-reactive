checkLanguage();

var context = $('body').data('context')

var ContainerSearch = React.createClass({
    getInitialState: function() {
        return { dictionary: "" };
    },
    setLang: function() {
        switch (window.localStorage.getItem("lang")) {
            case "de":
                window.globalDict = dictGer;
                window.localStorage.lang = "de";
                this.setState({dictionary: "de"});
                // globalFlushFilters();
                break;
            case "en":
                window.globalDict = dictEng;
                window.localStorage.lang = "en";
                this.setState({dictionary: "en"});
                // globalFlushFilters();
                break;
        }
    },
    render: function () {
        return (
            <div>
                <div className="row">
                    <div className="col-md-12 text-right">
                        <LangSwitcher onlangselect={this.setLang}/>
                    </div>
                </div>
                <div className="row">
                    <link rel="stylesheet" media="screen" href={context+"/assets/stylesheets/startPage.css"}>
                        <div className="col-md-12 search-widget">
                            <div class="row">
                                <img src={context+"/assets/images/imgpsh_fullsize_NoText.png"} className="bigLogo" alt="Logo_Description"/>
                                <h1 style={{color: "#456499"} }>{getTranslation("fuhsen")}</h1>
                            </div>
                            <div className="row">
                                <div className="col-md-12 text-center">
                                    <SearchForm id_class="form-search" lang = {this.state.dictionary}/>
                                </div>
                            </div>
                        </div>
                    </link>
                    <div className="row">
                        <div className="col-md-6 text-center">
                            <AccessTokenForm social_network="facebook" />
                        </div>
                        <div className="col-md-6 text-center">
                            <AccessTokenForm social_network="xing" />
                        </div>
                    </div>
                </div>

                <a href="http://www.bdk.de/lidakra" target="_blank" className="no-external-link-icon">
                    <div id="logo-mini" title={getTranslation("sponsored_by")}/>
                </a>

                <div id="contact-mini">
                    {getTranslation("need_help")}<a href="mailto:lidakra-support@@ontos.com">{getTranslation("contact")}<img class="thumbnail" src={context + "/assets/images/icons/help-desk-icon.png"} id="support-icon"/></a>
                </div>



            </div>
        );
    }
});

var KeywordsFile = React.createClass({
    handleFileSelection: function (evt)
    {
        var f = evt.target.files[0];
        var searches_array = []
        if (f) {
            var r = new FileReader();

            r.local_sources = this.props.sources;
            r.local_types = this.props.types;

            r.onload = function(e) {
                var contents = e.target.result;
                searches_array = contents.split("\n");
                for (var i = 0; i < searches_array.length; i++) {
                    var win = window.open(context+"/results?query="+searches_array[i]+"&sources="+this.local_sources+"&types="+this.local_types,'_blank');
                    win.focus();
                }
            }
            r.readAsText(f);
        } else {
            alert("Failed to load file");
        }
    },
    render: function () {
        return (
                    <div className="text-center">
                       <span className="btn btn-primary btn-file btn-md">
                           {getTranslation("select_file")} <input type="file" onChange={this.handleFileSelection}></input>
                       </span>
                    </div>
        )
    }
});

var LangSwitcher = React.createClass({
    preSetLang: function(lang, e) {
        window.localStorage.lang = lang
        this.props.onlangselect()
    },
    render: function () {
        let boundClickEng = this.preSetLang.bind(this, 'en');
        let boundClickGer = this.preSetLang.bind(this, 'de');

        if(window.localStorage.getItem("lang") === "de"){
            return (
                <div>
                    <b>Deutsch</b>
                    &#124;
                    <a href="#" onClick={boundClickEng}>English</a>
                </div>)
        } else {
            return (
                <div>
                    <a href="#" onClick={boundClickGer}>Deutsch</a>
                    &#124;
                    <b>English</b>
                </div>)
        }
    }
});

var SearchForm = React.createClass({
    getSelectionLabel: function(){
        var sources_list = this.getLabelsFromSelectedChecks(this.state.sources)
        var types_list = this.getLabelsFromSelectedAllowedChecks(this.state.types)

        var sources_label = ""
        var types_label = ""

        var prefix_sources_label = getTranslation("datasources");
        var prefix_types_label = getTranslation("types");
        if(sources_list.length === 0) {
            sources_label = prefix_sources_label  + ": "+ getTranslation("none") + "."
        }
        else if(sources_list.length === this.state.sources.length) {
            sources_label = prefix_sources_label + ": "+ getTranslation("all") + "."
        }else if(sources_list.length > 0) {
            sources_label = prefix_sources_label + ": (" + sources_list + ")."
        }else{
            alert("[Error] Well, seems like something went wrong...")
        }

        if(types_list.length === 0){
            types_label = prefix_types_label + ": "+ getTranslation("none") + "."
        }
        else if(types_list.length === this.state.types.length) {
            types_label = prefix_types_label + ": " + getTranslation("all") + "."
        }else if(types_list.length > 0) {
            types_label = prefix_types_label + ": (" + types_list + ")."
        }else{
            alert("[Error] Well, seems like something went wrong...")
        }

        return sources_label+" "+types_label
    },
    getLabelsFromSelectedChecks: function(checks_data) {
        var keys = [];
        for(var k in checks_data){
            if(checks_data[k]["selected"]){
                keys.push(checks_data[k]["id"])
            }
        }
        return keys
    },
    getLabelsFromSelectedAllowedChecks: function(checks_data) {
        var keys = [];
        for(var k in checks_data){
            if(checks_data[k]["selected"] && checks_data[k]["allowed"]){
                keys.push(checks_data[k]["id"])
            }
        }
        return keys
    },
    getKeysFromSelectedAllowedChecks(checks_data){
        var keys = [];
        for(var k in checks_data){
            if(checks_data[k]["selected"] && checks_data[k]["allowed"]){
                keys.push(checks_data[k]["key"])
            }
        }
        return keys
    },
    getKeysFromSelectedChecks: function(checks_data) {
        var keys = [];
        for(var k in checks_data){
            if(checks_data[k]["selected"]){
                keys.push(checks_data[k]["key"])
            }
        }
        return keys
    },
    getAllKeysFromChecks: function(checks_data) {
        var keys = [];
        for(var k in checks_data){
            keys.push(checks_data[k]["key"])
        }
        return keys
    },
    sourcesChanged: function(sources_data) {
        this.setState({ sources: sources_data, selectionLabel: this.getSelectionLabel()});
    },
    typesChanged: function(types_data) {
        this.setState({ types: types_data, selectionLabel: this.getSelectionLabel()},
            function () {

            });
    },
    onClick: function() {
        if(this.state.showSourcesTypesDiv) {
            this.setState({ showSourcesTypesDiv: false});
        } else {
            this.setState({ showSourcesTypesDiv: true});
        }
    },
    OnDocumentClick: function(e){
        if(e.target.className == "sel_button"){
            this.onClick();
        }
        else{
            if($("#filterList").has(e.target).length == 0) {
                if(this.state.showSourcesTypesDiv)
                    this.setState({ showSourcesTypesDiv: false});
            }
        }

    },
    componentDidMount: function(){
      document.addEventListener('click',this.OnDocumentClick);
    },
    componentWillUnmount: function() {
        document.removeEventListener('click', this.OnDocumentClick);
    },
    getAllowedTypes : function(sources){
        var allowed_types = []
        for(var idx in sources){
            Array.prototype.push.apply(allowed_types, this.state.typesForSource[sources[idx]]);
        }
        return allowed_types.filter(function(item,idx){
            return allowed_types.indexOf(item) === idx
        });
    },
    getInitialState: function() {
        var typesForSource= {};
        typesForSource["facebook"] = ["person"];
        typesForSource["twitter"] = ["person"];
        typesForSource["xing"] = ["person"];
        typesForSource["gplus"] = ["person","organization"];
        typesForSource["linkedleaks"] = ["person","organization"];
        typesForSource["gkb"] = ["person","organization"];
        typesForSource["ebay"] = ["product"];
        typesForSource["occrp"] = ["document"];
        typesForSource["tor2web"]  = ["website"];
        typesForSource["elasticsearch"]  = ["website"];

        return { showSourcesTypesDiv: false, sources: [] , types: [],typesForSource: typesForSource};
    },
    handleSubmit : function(e){
        var selected_sources_list = this.getKeysFromSelectedChecks(this.state.sources);
        var selected_types_list = this.getKeysFromSelectedAllowedChecks(this.state.types);
        if(selected_sources_list.length === 0 || selected_types_list.length === 0){
            alert("Datasource or EntityType is not selected!!.Please Select at least one Datasource and entitytype");
            return false;
        }
        return true;
    },
    render: function() {

        var selected_sources_list = this.getKeysFromSelectedChecks(this.state.sources)
        var allowed_types = this.getAllowedTypes(selected_sources_list);
        var selected_types_list = this.getKeysFromSelectedAllowedChecks(this.state.types)

        var selected_sources = selected_sources_list.length === 0 ? this.getAllKeysFromChecks(this.state.sources): selected_sources_list
        var selected_types = selected_types_list.length === 0 ? this.getAllKeysFromChecks(this.state.types): selected_types_list

        var floatingDivStyle = this.state.showSourcesTypesDiv ? "col-md-4 floatingSelChecks text-left" : "col-md-4"

        if(this.props.id_class === "form-search-header"){
            floatingDivStyle = this.state.showSourcesTypesDiv ? "col-md-4 floatingSelChecks-header text-left" : "col-md-4"
        }

        if(this.props.keyword)
        {
            return (
                <div>
                    <form method="get" id={this.props.id_class} role="search" action={context+"/results"} onSubmit={this.handleSubmit}>
                        <div>
                            <label ><span>Search: </span></label>
                            <input type="text" name="query" defaultValue={this.props.keyword} placeholder={getTranslation("yoursearch")}/>&nbsp;
                            <input type="hidden" name="sources" value={selected_sources}/>
                            <input type="hidden" name="types" value={selected_types}/>
                            <button type="submit">&nbsp;</button>
                        </div>
                        <div>
                            <div className="floatingSelText-header">
                                {this.getSelectionLabel()}
                                <img className="sel_button" src={context+"/assets/images/icons/arrow_down.png"}>
                                </img>
                            </div>
                        </div>
                    </form>
                    <div class="row">
                        <div className="col-md-3"/>
                        <div className={floatingDivStyle}>
                            <div className="row" id="filterList">
                                <div className="col-md-6 separator">
                                    <FilterCheckList filterType="datasources" lang = {this.props.lang} AllowedTypes = {[]} onSourceChangedFunction={this.sourcesChanged} show={this.state.showSourcesTypesDiv}/>
                                </div>
                                <div className="col-md-6">
                                    <FilterCheckList filterType="entitytypes" lang = {this.props.lang} AllowedTypes = {allowed_types} onSourceChangedFunction={this.typesChanged} show={this.state.showSourcesTypesDiv}/>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-5"/>
                    </div>
                </div>

            );
        }

        return (
            <div>
                <div className="row">
                    <div className="col-md-3"/>
                    <div className="col-md-6">
                        <form method="get" id={this.props.id_class} role="search" action={context+"/results"} onSubmit={this.handleSubmit}>
                            <div>
                                <label ><span>Search: </span></label>
                                <input type="search" name="query" placeholder={getTranslation("yoursearch")}/>&nbsp;
                                <input type="hidden" name="sources" value={selected_sources}/>
                                <input type="hidden" name="types" value={selected_types}/>
                                <button type="submit">&nbsp;</button>
                            </div>
                            <div>
                                <div className="floatingSelText">
                                    {this.getSelectionLabel()}
                                    <img className="sel_button" src={context+"/assets/images/icons/arrow_down.png"}>
                                    </img>
                                </div>
                            </div>
                        </form>
                    </div>
                    {/*<div className="col-md-1 vertical-separator" title="Search with 1 keyword <- OR ->Search with 1 or more keywords"/>*/}
                    {/*<div className="col-md-1 ">*/}
                        {/*<KeywordsFile sources={selected_sources} types={selected_types}/>*/}
                    {/*</div>*/}
                </div>
                <div class="row">
                    <div className={floatingDivStyle}>
                        <div className="row" id="filterList">
                            <div className="col-md-6 separator">
                                <FilterCheckList filterType="datasources" lang = {this.props.lang} AllowedTypes = {[]} onSourceChangedFunction={this.sourcesChanged} show={this.state.showSourcesTypesDiv}/>
                            </div>
                            <div className="col-md-6">
                                <FilterCheckList filterType="entitytypes" lang = {this.props.lang} AllowedTypes = {allowed_types} onSourceChangedFunction={this.typesChanged} show={this.state.showSourcesTypesDiv}/>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
});

var FilterCheckList = React.createClass({
    loadListFromServer: function (filter) {

        var list_url = context+"/engine/api/schema/"+filter

        var previousDataList = []

        if(this.props.filterType === "datasources") {
            if (typeof sourcesDirty !== 'undefined') {
                previousDataList= sourcesDirty.split(',');
            }
        }

        if(this.props.filterType === "entitytypes") {
            if(typeof typesDirty !== 'undefined'){
                previousDataList= typesDirty.split(',');
            }
        }

        $.ajax({
            url: list_url,
            dataType: 'json',
            cache: false,
            success: function (list_data) {
                var processed_data = [];
                for(var k in list_data["@graph"]){
                    var current_label = list_data["@graph"][k]["rdfs:label"]
                    var current_key = list_data["@graph"][k]["fs:key"]
                    var checked = false

                    if(list_data["@graph"].length >= previousDataList.length) {
                        checked = $.inArray(current_key, previousDataList) > -1 || previousDataList.length == 0 ? true : false
                    }

                    if(Object.prototype.toString.call(current_label) === '[object Array]'){
                        for(var j in current_label){
                            if(current_label[j]["@language"] === window.localStorage.getItem("lang")){
                                processed_data.push({ id: current_label[j]["@value"], selected: checked, key: current_key, allowed: true})
                            }
                        }
                    }else{
                        processed_data.push({ id: current_label, selected: checked, key: current_key,allowed: true})
                    }
                }
                // if(this.props.filterType === "entitytypes"){
                //     processed_data = this.updateChecks(processed_data,this.props.AllowedTypes);
                // }
                this.setState({data: processed_data});
                this.props.onSourceChangedFunction(processed_data)
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(list_url, status, err.toString());
            }.bind(this)
        });
    },
    updateChecks: function (checks,allowedTypes) {
        if(checks !== undefined) {
            return checks.map(function (d) {
                var found = allowedTypes.indexOf(d.key) > -1;
                return {
                    id: d.id,
                    selected: d.selected,
                    allowed: found,
                    key: d.key
                };
            });
        }
        return undefined;
    },
    componentWillReceiveProps: function (nextProps) {
        if(nextProps.lang != this.props.lang)
            this.loadListFromServer(this.props.filterType);
        if(nextProps.AllowedTypes.length !== this.props.AllowedTypes.length && this.props.filterType === "entitytypes"){
            var updatedData = this.updateChecks(this.state.data,nextProps.AllowedTypes);
            this.setState({data: updatedData});
        }
    },
    componentDidMount: function () {
        this.loadListFromServer(this.props.filterType);
    },
    __changeSelection: function(id) {
        var state = this.state.data.map(function(d) {
            return {
                id: d.id,
                selected: (d.id === id ? !d.selected : d.selected),
                allowed: d.allowed,
                key: d.key
            };
        });
        var allselected = state.every(function(item){
           return item.selected;
        });
        this.props.onSourceChangedFunction(state)
        this.setState({ data: state, selectAll:allselected });
    },
    __changeSelectAll: function(){
        var state = this.state.data.map(function(d) {
            return {
                id: d.id,
                selected: !this.state.selectAll,//!d.selected,//(d.id === id ? !d.selected : d.selected),
                allowed: d.allowed,
                key: d.key
            };
        },this);
        this.props.onSourceChangedFunction(state)
        this.setState({ data: state , selectAll: !this.state.selectAll});
    },
    getInitialState: function() {
        return { data: undefined, selectAll: true };
    },
    render: function() {
        if (this.props.show) {
            if (this.state.data) {
                var filter_title = getTranslation(this.props.filterType);//(this.props.filterType).charAt(0).toUpperCase() + (this.props.filterType).slice(1);

                var checks = this.state.data.map(function(d) {
                    if(d.allowed) {
                        return (
                            <div>
                                &emsp;<input type="checkbox" checked={d.selected}
                                             onChange={this.__changeSelection.bind(this, d.id)}/>
                                {d.id}
                                <br />
                            </div>
                        );
                    }
                    else{
                        return (
                            <div>
                                &emsp;<s><input type="checkbox" checked="false" disabled="true"
                                                     onChange={this.__changeSelection.bind(this, d.id)}/>
                                {d.id}
                            </s>
                                <br />
                            </div>
                        );
                    }
                }.bind(this));
                return (
                    <div>
                         <p className="thick">
                             <input type="checkbox" checked={this.state.selectAll} onChange={this.__changeSelectAll}/>
                             {filter_title+":"}</p>
                        {checks}
                    </div>
                );
            }
            return <div className="row">
                <div className="col-md-12 text-center">
                    <h2><img src={context+"/assets/images/ajaxLoader.gif"}/>{getTranslation("bittewarten")}</h2>
                </div>
            </div>;
        }

        return null;
    }
});

var AccessTokenForm = React.createClass({
    loadTokenLifeLength: function () {

        var social_network_url = context+"/"+this.props.social_network+"/getTokenLifeLength"

        $.ajax({
            url: social_network_url,
            dataType: 'json',
            cache: false,
            success: function (lifelength) {
                this.setState({token_life_length: lifelength["life_length"]});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    getInitialState: function () {
        return {token_life_length: null};
    },
    componentDidMount: function () {
        this.loadTokenLifeLength();
    },
    render: function() {

        var social_net_upper_case = (this.props.social_network).charAt(0).toUpperCase() + (this.props.social_network).slice(1);

        if(this.state.token_life_length) {
            if(this.state.token_life_length === "-1") {
                return (
                    <div className="accessTokenDiv" align="center">
                        {getTranslation("novalidtkfound_pre")+social_net_upper_case+getTranslation("novalidtkfound_post")}
                        <br/>
                        <br/>
                        <form action={context+"/"+this.props.social_network+"/getToken"} method="get">
                            <button>&nbsp;{getTranslation("newtoken")}&nbsp;</button>
                        </form>

                    </div> )
            }
            else if(this.state.token_life_length < 60) {
                return (
                    <div align="center">
                        <p>{social_net_upper_case+getTranslation("validtkfound")} {this.state.token_life_length} {getTranslation("minutes")}.
                        </p>
                    </div> )
            }
            else if(this.state.token_life_length < 1440) {
                return (
                    <div align="center">
                        <p>{social_net_upper_case+getTranslation("validtkfound")} {this.state.token_life_length} {getTranslation("hours")}.
                        </p>
                    </div> )
            }
            else {
                return (
                    <div align="center">
                        <p>{social_net_upper_case+getTranslation("validtkfound")} {Math.floor((this.state.token_life_length/60)/24)} {getTranslation("days")}.</p>
                    </div> )
            }
        }
        return (
            <div align="center">
                {getTranslation("checkingtoken")}
            </div> )
    }
});

var SupportContact = React.createClass({
    render: function () {
        return (
            <div id="contact-footer">
                <img class="thumbnail" src={context + "/assets/images/icons/help-desk-icon.png"} id="support-icon"/>
                <h6>{getTranslation("need_help")}</h6>
                <h6><a class="no-external-link-icon" href="mailto:lidakra-support@ontos.com">{getTranslation("contact")}</a></h6>
            </div>
        );
    }
});

React.render(<ContainerSearch />, document.getElementById('containersearch'));