var FavoritesContainer = React.createClass({
    getInitialState: function () {
        return {data: undefined};
    },
    /*loadProfileFromServer: function(uid,eUri,eType){
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
    },*/
    componentDidMount: function (){
        //this.loadProfileFromServer(this.props.uid,this.props.eUri,this.props.entityType);
    },
    render: function () {
        return (
            <div className="container">
                <div className="row" id="header-main-row">
                 <div id ="profile_container">
                            Hello World!!
                 </div>
                </div>
            </div>
        );
    }
});

React.render(
    <FavoritesContainer/>, document.getElementById('skeleton'));