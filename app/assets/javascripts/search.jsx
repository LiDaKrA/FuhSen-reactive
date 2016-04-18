var SearchForm = React.createClass({
    render: function() {
        return (
            <form method="get" role="search" id="form-search" action="http://localhost:9000/ldw/v1/restApiWrapper/id/twitter/search">
                <label><span>Search_text_field</span></label>
                <input type="search" class="query" name="query" placeholder="Persons, Organizations or Products"/>
                <button type="submit">Go</button>
            </form>
        );
    }
});

React.render(<SearchForm />, document.getElementById('searchform'));

var FacebookForm = React.createClass({
    render: function() {
        return (
            <form action="/facebook/getToken" method="get">
                <button>Retrieve a new access token</button>
            </form>
        );
    }
});

React.render(<FacebookForm />, document.getElementById('facebookform'));
