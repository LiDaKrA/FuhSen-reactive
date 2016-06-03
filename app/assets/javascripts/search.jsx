if(window.globalDict === undefined){
    window.globalDict = dictGer;
    function getTranslation(toTranslate){
        if(toTranslate in globalDict){
            return globalDict[toTranslate];
        }
        else return toTranslate;
    }
    window.getTranslation = getTranslation;
}

var ContainerSearch = React.createClass({
    getInitialState: function () {
        return {
            dictionary: "ger"
        }
    },
    // event handler for language switch
    // change dictionary then update state so the page notices the change
    setLang: function () {
        var lang = document.getElementById("langselect").value;
        switch (lang) {
            case "german":
                globalDict = dictGer;
                this.setState({dictionary: "ger"});
                globalFlushFilters();
                break;
            case "english":
                globalDict = dictEng;
                this.setState({dictionary: "eng"});
                globalFlushFilters();
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
                        <link rel="stylesheet" media="screen" href="http://localhost:9000/assets/stylesheets/startPage.css">
                        <div className="col-md-12 search-widget">
                            <div class="row">
                                <img src="http://localhost:9000/assets/images/logoBig.png" class="bigLogo" alt="Logo_Description"/>
                            </div>
                        <div className="row">
                            <SearchForm id_class="form-search"/>
                        </div>
                        </div>
                        </link>
                        <div className="row">
                            <div className="col-md-12 text-center">
                                <FacebookForm />
                            </div>
                        </div>
                    </div>
                </div>
        );
    }
});


var LangSwitcher = React.createClass({
    render: function () {
        return (
                    <form action="" id="langselectform">
                        <select name="lang" id="langselect" onChange={this.props.onlangselect}>
                            <option value="german">Deutsch</option>
                            <option value="english">English</option>
                        </select>
                    </form>
        );
    }
});


var SearchForm = React.createClass({
    render: function() {
        return (
            <form method="get" role="search" id={this.props.id_class} action="/results">
                <label><span>Search_text_field</span></label>
                <input type="search" class="query" name="query" placeholder={getTranslation("yoursearch")}/>
                <button type="submit">Go</button>
            </form>
        );
    }
});

var FacebookForm = React.createClass({
    getInitialState: function () {
        return {token_life_length: "-1"};
    },
    componentDidMount: function () {
        this.handleClick();
    },
    handleClick: function () {

        var searchUrl = "/facebook/getToken";

        $.ajax({
            url: searchUrl,
            dataType: 'json',
            cache: false,
            success: function (data) {
                this.setState({token_life_length : data});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    render: function() {
        if(this.state.token_life_length === "-1") {
            return (
                <div align="center">
                    <p>{getTranslation("novalidfbtkfound")}
                        <br/>
                        <br/>
                        <button onClick={this.handleClick}>{getTranslation("newfbtoken")}</button>
                    </p>
                </div> )
        }
        else if(this.state.token_life_length.toInt < 24) {
            return (
                <div align="center">
                    <p>{getTranslation("validfbtkfound")} {this.state.token_life_length} {getTranslation("hours")}.
                    </p>
                </div> )
        }
        else {
            return (
                <div align="center">
                    <p>{getTranslation("validfbtkfound")} {this.state.token_life_length.toInt/24} {getTranslation("days")}.</p>
                </div> )
        }
    }
});

React.render(<ContainerSearch />, document.getElementById('containersearch'));
