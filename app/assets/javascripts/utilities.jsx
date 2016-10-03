var RichText = React.createClass({
    OnClickMoreDetails: function (){
        alert(this.props.label + ":\n" + this.props.text);
        return false;
    },
    render: function () {
        var text = this.props.text;
        var moredata = false;
        var maxLenght = parseInt(this.props.maxLength);
        if($.trim(text).length > maxLenght){
            text =  text.substring(0,maxLenght);
            moredata = true;
        }
        return (
                <span>
                    {text}
                    {moredata ? <a href="#" onClick={this.OnClickMoreDetails}> ...read details...</a> : null}
                </span>
        );
    }
});