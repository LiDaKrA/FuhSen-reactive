var SearchBar = React.createClass({
    render: function() {
        return (
            <form className="searchForm" id="searchform" action="results.html"  method="GET">
                <input type="text" id="searchinput" name="q" placeholder="Here one" class="form-control"/>
                <button class="btn btn-embossed btn-primary" id="btnsearchsubmit">
                    Search
                </button>
            </form>
        );
    }
});

React.render(<SearchBar />, document.getElementById('searchbar'));