var SearchForm = React.createClass({
    render: function() {
        return (
            <form method="get" role="search" id={this.props.id_class} action="/results">
                <label><span>Search_text_field</span></label>
                <input type="search" class="query" name="query" placeholder="Persons, Organizations or Products"/>
                <button type="submit">Go</button>
            </form>
        );
    }
});

React.render(<SearchForm id_class="form-search"/>, document.getElementById('searchform'));

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
