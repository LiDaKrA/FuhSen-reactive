var Container = React.createClass({
    render: function () {
        return (
            <LogicKeeper></LogicKeeper>
        );
    }
});

var LogicKeeper = React.createClass({
    render: function () {
        return (
            <div className="row search-results-container">
                <div className="col-md-3 facets-container hidden-phone">
                    <div className="facets-head">
                        <h3>Ergebnisse filtern</h3>
                    </div>
                    <div className="js facets-list bt bb">
                        <div className="facets-item bt bb bl br">
                            <a className="h3" href="#" data-fctname="person_gender_fct">Geschlecht</a>
                        </div>
                        <div className="facets-item bt bb bl br">
                            <a className="h3" href="#" data-fctname="person_birthday_fct">Geburtstag</a>
                        </div>

                        <div className="facets-item bt bb bl br">
                            <a className="h3" href="#" data-fctname="person_occupation_fct">Beruf</a>
                        </div>

                        <div className="facets-item bt bb bl br">
                            <a className="h3" href="#" data-fctname="person_livesat_fct">Lebt in</a>
                        </div>

                        <div className="facets-item bt bb bl br">
                            <a className="h3" href="#" data-fctname="person_worksat_fct">Arbeitet bei</a>
                        </div>

                        <div className="facets-item bt bb bl br">
                            <a className="h3" href="#" data-fctname="person_studiesat_fct">Studium an</a>
                        </div>
                    </div>
                </div>

                <div className="col-md-9">
                    <div id="results-paginator-options" className="results-paginator-options">
                        <div class="off result-pages-count"></div>
                        <div className="row">
                            <div className="col-md-12">
                                <ul className="tabulator list-inline">
                                    <li>
                                        <span className="total-results">50</span>
                                        <span className="total-results-label"> Ergebnisse:</span>
                                    </li>
                                    <li>
                                        <a href="#" className="active-link">
                                            <p><strong>Personen</strong></p>
                                        </a>
                                    </li>
                                    <li>
                                        <a href="#" className="">
                                            Organisationen
                                        </a>
                                    </li>
                                    <li>
                                        <a href="#" className="">
                                            Produkte
                                        </a>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>

                    <div className="search-results-content">
                        <div className="row">
                            <div className="col-md-12">
                                <ul id="search-results" className="search-results">
                                    <ul className="results-list list-unstyled">
                                        <ResultElement
                                            name="Diego Collarana"
                                            gender="Male"
                                            addrss="Bonn/DE"
                                            url="https://pbs.twimg.com/profile_images/2175489201/Dic01.png"
                                            social_src="http://localhost:9000/assets/images/datasources/googleplus.png">
                                        </ResultElement>
                                        <ResultElement
                                            name="Camilo Morales"
                                            gender="Male"
                                            addrss="Bonn/DE"
                                            url="https://scontent.xx.fbcdn.net/hprofile-xpa1/v/t1.0-1/p50x50/21171_10155422530195055_1584223177974390414_n.jpg?oh=87385fa8dc9a98da04bb2b135292a74f&oe=57C036F3"
                                            social_src="http://localhost:9000/assets/images/datasources/facebook.png">
                                        </ResultElement>
                                    </ul>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
});

var ResultElement = React.createClass({
    render: function () {
        return (
            <li className="item bt">
                <div className="summary row">
                    <div className="thumbnail-wrapper col-md-2">
                        <div className="thumbnail">
                            <img src={this.props.url} height="60px" width="75px"/>
                        </div>
                    </div>
                    <div className="summary-main-wrapper col-md-8">
                        <TableRow name={this.props.name} gender={this.props.gender} addrss={this.props.addrss}></TableRow>
                    </div>
                    <div class="thumbnail-wrapper col-md-1">
                        <div class="thumbnail">
                            <img src={this.props.social_src} alt="Information from FB" height="45" width="45"/>
                        </div>
                    </div>
                </div>
            </li>
        );
    }
});

var TableRow = React.createClass({
    render: function () {
        return (
            <div className="summary-main">
                <h2 className="title">
                    {this.props.name}
                </h2>
                <div className="subtitle">
                    <p>Gender: {this.props.gender}</p>
                    <p>Address: {this.props.addrss}</p>
                </div>
            </div>
        );
    }
});

React.render(<Container />, document.getElementById('skeleton'));

React.render(<SearchForm id_class="form-search-header"/>, document.getElementById('searchform'));