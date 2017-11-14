function getValue(property) {
    if(Array.isArray(property)){
        return property[0];
    }
    else
        return property;
}

var ProfileContainer = React.createClass({
    getInitialState: function () {
        return {data: undefined};
    },
    loadProfileFromServer: function(uid,eUri,eType){
        var url = context + "/engine/api/entitysummarization/" + uid + "/summarize?uri=" + eUri + "&entityType=" + eType;
        $.ajax({
            url: url,
            dataType: 'json',
            cache: false,
            success: function (response) {
                if(response["@context"] !== undefined) {
                    delete response["@context"];
                }
                var data_to_handle = response;
                this.setState({
                   data: data_to_handle
                });
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    componentDidMount: function (){
        this.loadProfileFromServer(this.props.uid,this.props.eUri,this.props.entityType);
    },
    render: function () {
        return (
            <div className="container">
                <div className="row" id="header-main-row">
                 <div id ="profile_container">
                            <ProfileHeader image={this.state.data !== undefined ? (this.state.data["image"] !== undefined ? this.state.data["image"] : this.state.data["fs:image"]) : undefined} name={this.state.data !== undefined ? this.state.data["fs:title"] : undefined}/>
                            <ProfileBody data = {this.state.data}/>
                    </div>
                </div>
            </div>
        );
    }
});

var ProfileHeader = React.createClass({
    render: function () {
        var singleName = getValue(this.props.name);
        var singleImg = getValue(this.props.image);
        return (
            <div id="profile_container_top">
                <div id="profile_image">
                    <img className="thumbnail" src={singleImg} width="90" height="90"/>
                </div>
                <div id="profile_summary">
                    <div className="header">
                        <span className="highlight">
                            {singleName}
                        </span>
                    </div>
                </div>
            </div>
        );
    }
});

var ProfileBody = React.createClass({
    render: function () {
        var profileSections = (this.props.data !== undefined ? Object.keys(this.props.data).map(function(key,index){
            var data_items = this.props.data[key];
            if(!Array.isArray(this.props.data[key]))
                data_items = [this.props.data[key]];
            return(
               <ProfileSection header={key} data={data_items}/>
           )
        },this) : undefined);
        return (
            <div id="profile_container_middle">
                {profileSections}
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
            <div className="row-line">
                <div className="field_label">
                    <div className="hidden-xs hidden-sm">
                        {/*<i className="fa fa-suitcase hidden-sm hidden-xs"/>&nbsp;*/}
                        <span><b>{this.props.header}</b></span>
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
    <ProfileContainer uid = {uid} eUri = {eUri} entityType = {eType}/>
    , document.getElementById('skeleton'));