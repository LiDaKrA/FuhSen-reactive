// var context = $('body').data('context');
var ContainerHeader = React.createClass({
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
    render:function(){
        return(
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
                                        <SearchForm id_class="form-search-header" keyword={""}/>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </nav>
                </div>
            </div>
        );
    }
});
var ProfileContainer = React.createClass({
    render: function () {
        var image = "https://media.licdn.com/media/p/4/000/13e/336/35bb4dc.jpg";
        var name = "Diego Collorana";
        return (
            <div>
                <ContainerHeader />
                {/*<div className="row search-results-container">*/}
                 <div id ="profile_container" className="profile">
                            <ProfileHeader image={image} name={name}/>
                            <ProfileBody />
                    </div>
                {/*</div>*/}
            </div>
        );
    }
});

var ProfileHeader = React.createClass({
    render: function () {
        return (
            <div id="profile_container_top">
                <div id="profile_image">
                    <img className="thumbnail b-loaded" src={this.props.image} width="90" height="90"/>
                </div>
                <div id="profile_summary">
                    <div className="header">
                        <span className="highlight">
                            {this.props.name}
                        </span>
                    </div>
                </div>
            </div>
        );
    }
});

var ProfileBody = React.createClass({
    render: function () {
        var header = "CAREER";
        var data_items = ["Researcher at Fraunhofer (since 2016)", "Researcher at EIS Group,Uni Bonn (since 2015)", "Software Engineer at X (2014-2015)"];
        return (
            <div id="profile_container_middle">
                <ProfileSection header={header} data={data_items}/>
                <ProfileSection header={header} data={data_items}/>
            </div>
        );
    }
});

var ProfileSection = React.createClass({
    render: function () {
        var data_items = this.props.data.map(function (item) {
            return (<li><span>{item}</span><br/></li>);
        });
        return (
            <div className="row-line group">
                <div className="field_label">
                    <div className="hidden-xs hidden-sm">
                        <i className="fa fa-suitcase hidden-sm hidden-xs"/>&nbsp;
                        <span>{this.props.header}:</span>
                    </div>
                </div>
                <div className="values">
                    <ul className="jobs">
                        {data_items}
                    </ul>
                 </div>
                </div>
        );
    }
});
React.render(
    <ProfileContainer />
    , document.getElementById('skeleton'));