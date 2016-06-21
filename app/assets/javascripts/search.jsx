checkLanguage();

var ContainerSearch = React.createClass({
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
                    <div className="row">
                        <div className="col-md-12 text-right">
                            <LangSwitcher onlangselect={this.setLang}/>
                        </div>
                    </div>
                    <div className="row">
                        <link rel="stylesheet" media="screen" href="/assets/stylesheets/startPage.css">
                        <div className="col-md-12 search-widget">
                            <div class="row">
                                <img src="/assets/images/logoBig.png" class="bigLogo" alt="Logo_Description"/>
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
        if(window.localStorage.getItem("lang") === "ger"){
            return (
                <form action="" id="langselectform">
                    <select name="lang" id="langselect" onChange={this.props.onlangselect}>
                        <option value="german" selected>Deutsch</option>
                        <option value="english">English</option>
                    </select>
                </form>)
        } else {
            return (
                <form action="" id="langselectform">
                    <select name="lang" id="langselect" onChange={this.props.onlangselect}>
                        <option value="german">Deutsch</option>
                        <option value="english" selected>English</option>
                    </select>
                </form>)
        }
    }
});


var SearchForm = React.createClass({
    render: function() {
        if(this.props.keyword)
        {
            return (
                <form method="get" id={this.props.id_class} role="search" action="/results">
                    <div>
                        <label ><span>Search: </span></label>
                        <input type="text" name="query" defaultValue={this.props.keyword} placeholder={getTranslation("yoursearch")}/>&nbsp;
                        <button type="submit">&nbsp;</button>
                    </div>
                </form>
            );
        }

        return (
            <form method="get" id={this.props.id_class} role="search" action="/results">
                <div>
                    <label ><span>Search: </span></label>
                    <input type="search" name="query" placeholder={getTranslation("yoursearch")}/>&nbsp;
                    <button type="submit">&nbsp;</button>
                </div>
            </form>
        );
    }
});

var FacebookForm = React.createClass({
    loadTokenLifeLength: function () {
        $.ajax({
            url: "/facebook/getTokenLifeLength",
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
        if(this.state.token_life_length) {
            if(this.state.token_life_length === "-1") {
                return (
                    <div align="center">
                        {getTranslation("novalidfbtkfound")}
                            <br/>
                            <br/>
                            <form action="/facebook/getToken" method="get">
                                <button>{getTranslation("newfbtoken")} </button>
                            </form>

                    </div> )
            }
            else if(this.state.token_life_length < 24) {
                return (
                    <div align="center">
                        <p>{getTranslation("validfbtkfound")} {this.state.token_life_length} {getTranslation("hours")}.
                        </p>
                    </div> )
            }
            else {
                return (
                    <div align="center">
                        <p>{getTranslation("validfbtkfound")} {Math.floor(this.state.token_life_length/24)} {getTranslation("days")}.</p>
                    </div> )
            }
        }
        return (
            <div align="center">
                {getTranslation("checkingfbtoken")}
            </div> )
    }
});

React.render(<ContainerSearch />, document.getElementById('containersearch'));
