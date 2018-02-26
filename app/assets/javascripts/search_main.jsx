checkLanguage();

var context = $('body').data('context');

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
                            <div>
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
                        {/*<div className="col-md-4 text-center">
                            <AccessTokenForm social_network="vk" />
                        </div>*/}
                        <div className="col-md-6 text-center">
                            <AccessTokenForm social_network="xing" />
                        </div>
                    </div>
                </div>

                <a href="http://www.bdk.de/lidakra" target="_blank" className="no-external-link-icon">
                    <div id="logo-mini" title={getTranslation("sponsored_by")}/>
                </a>

                <div id="contact-mini">
                    {getTranslation("need_help")}<a href="mailto:lidakra-support@ontos.com">{getTranslation("contact")}<img src={context + "/assets/images/icons/help-desk-icon.png"} id="support-icon"/></a>
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
        this.checkHash();
        this.loadTokenLifeLength();
    },
    checkHash: function () {
        if (window.location.hash) {
            var hash = window.location.hash.substring(1);
            if(hash.includes("access_token") && this.props.social_network === "vk"){
                $.ajax({
                    url: context + "/" + this.props.social_network + "/code2tokenV?" + hash,
                    success: function (lifelength) {
                        this.setState({token_life_length: lifelength["life_length"]});
                    }.bind(this),
                    error: function (xhr, status, err) {
                        console.error(this.props.url, status, err.toString());
                    }.bind(this)
                });

            }
        }
    },
    render: function() {

        var social_net_upper_case = (this.props.social_network).charAt(0).toUpperCase() + (this.props.social_network).slice(1);

        if(this.state.token_life_length) {
            if(this.state.token_life_length === "-1") {
                return (
                    <div className="accessTokenDiv" align="center">
                        {getTranslation("novalidtkfound_pre")}<span className="socialNetworkName">{social_net_upper_case}</span>{getTranslation("novalidtkfound_post")}
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
            else if(this.state.token_life_length >= 1.2e15) {
                return (
                    <div align="center">
                        <p>{social_net_upper_case+getTranslation("validtkforever")} .
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
                <img src={context + "/assets/images/icons/help-desk-icon.png"} id="support-icon"/>
                <h6>{getTranslation("need_help")}</h6>
                <h6><a href="mailto:lidakra-support@ontos.com">{getTranslation("contact")}</a></h6>
            </div>
        );
    }
});

React.render(<ContainerSearch />, document.getElementById('containersearch'));