var ContainerSearch = React.createClass({
    // event handler for language switch
    // change dictionary then update state so the page notices the change
    setLang: function () {
        var lang = document.getElementById("langselect").value;
        switch (lang) {
            case "german":
                window.globalDict = dictGer;
                this.setState({dictionary: "ger"});
                globalFlushFilters();
                break;
            case "english":
                window.globalDict = dictEng;
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
