var SearchFormHeader = React.createClass({
    render: function() {
        return (
            <form method="get" role="search" id="form-search-header" action="http://localhost:9000/ldw/v1/restApiWrapper/id/twitter/search">
                <input type="search" class="query" name="query" placeholder="Persons, Organizations or Products"/>
                <button type="submit"></button>
            </form>
        );
    }
});

React.render(<SearchFormHeader />, document.getElementById('searchformheader'));