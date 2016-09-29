/**
 * Created by Ahmed on 9/24/2016.
 */
var ResultsTable = React.createClass({

    render: function () {
        var resultsNodesSorted = this.props.data["@graph"].sort(compareRank);

        var resultsNodes = resultsNodesSorted.map(function (result,i) {
            if (result["@type"] === "foaf:Person") {
                return (
                    <PersonResultRow
                        id = {i}
                        img={result.image}
                        name={result["fs:title"]}
                        source={result["fs:source"]}
                        alias={result["fs:alias"]}
                        location={result["fs:location"]}
                        label={result["fs:label"]}
                        comment={result["fs:comment"]}
                        gender={result["foaf:gender"]}
                        occupation={result["fs:occupation"]}
                        birthday={result["fs:birthday"]}
                        country={result["fs:country"]}
                        webpage={result.url}
                        active_email={result["fs:active_email"]}
                        wants={result["fs:wants"]}
                        haves={result["fs:haves"]}
                        top_haves={result["fs:top_haves"]}
                        interests={result["fs:interests"]}
                    >
                    </PersonResultRow>
                );
             } //else if (result["@type"] === "foaf:Organization") {
            //     return (
            //         <OrganizationResultElement
            //             img={result.image}
            //             title={result["fs:title"]}
            //             source={result["fs:source"]}
            //             label={result["fs:label"]}
            //             comment={result["fs:comment"]}
            //             country={result["fs:country"]}
            //             location={result["fs:location"]}
                        {/*webpage={result.url}>*/}
                    {/*</OrganizationResultElement>*/}
                {/*);*/}
            {/*} else if (result["@type"] === "gr:ProductOrService") {*/}
                {/*return (*/}
                    {/*<ProductResultRow*/}
                        {/*img={result.image}*/}
                        {/*title={result["fs:title"]}*/}
                        {/*source={result["fs:source"]}*/}
                        {/*location={result["fs:location"]}*/}
                        {/*country={result["fs:country"]}*/}
                        {/*price={result["fs:price"]}*/}
                        {/*condition={result["fs:condition"]}*/}
                        {/*webpage={result.url}>*/}
                    {/*</ProductResultRow>*/}
                {/*);*/}
            {/*} else if (result["@type"] === "foaf:Document") {*/}
                {/*return (*/}
                    {/*<WebResultElement*/}
                        {/*img={context + "/assets/images/datasources/TorLogo.png"}*/}
                        {/*onion_url={result.url}*/}
                        {/*comment={result["fs:excerpt"]}*/}
                        {/*source={result["fs:source"]}*/}
                        {/*crawled={already_crawled}>*/}
                    {/*</WebResultElement>*/}
            //     );
            // } else if (result["@type"] === "fs:Document") {
            //     return (
            //         <DocumentResultElement
            //             label={result["fs:title"]}
            //             comment={result["fs:comment"]}
            //             webpage={result.url}
            //             country={result["fs:country"]}
            //             language={result["fs:language"]}
            //             filename={result["fs:file_name"]}
            //             extension={result["fs:filetype"]}
            //             source={result["fs:source"]}>
            //         </DocumentResultElement>
            //     );
            // }
        });

        return (
            <div className="table-responsive">
                <table className="table table-bordered table-hover table-condensed" styles="width:100%" data-toggle="table" data-detail-view="true" data-detail-formatter="detailFormatter">
                    <thead>
                    <TableHeader type={this.props.type}/>
                    </thead>
                    <tbody>
                    {resultsNodes}
                    </tbody>
                </table>
            </div>
        );
    }
});
var TableHeader = React.createClass({
    OnClickCheckBox: function(){
        $(".checkBoxClass").click();
    },
    render: function () {

        if (this.props.type == "person")
        {
            return (

                <tr>
                    <th>
                        <input type="checkbox" id="selectall" onClick={this.OnClickCheckBox}/>
                    </th>
                    <th></th>
                    <th>Name</th>
                    <th>{getTranslation("location")}</th>
                    <th>{getTranslation("nick")}</th>
                    <th>{getTranslation("gender")}</th>
                    <th>{getTranslation("occupation")}</th>
                    <th>{getTranslation("birthday")}</th>
                    <th>{getTranslation("country")}</th>
                    <th>Label</th>
                    <th>{getTranslation("link")}</th>
                    <th>{getTranslation("active_email")}</th>
                    <th>{getTranslation("wants")}</th>
                    <th>{getTranslation("haves")}</th>
                    <th>{getTranslation("top_haves")}</th>
                    <th>{getTranslation("interests")}</th>
                </tr>

            );
        }
    }

});
var PersonResultRow = React.createClass({
    OnClickCheckBox: function () {
      alert($("#check"+this.props.id).is(":checked"));
    },
    OnClickCell : function () {
        $("#check"+this.props.id).click();
    },
    OnCheckBoxChange : function () {
        var checkBox = document.getElementById("check"+this.props.id);
        var currentRow = checkBox.parentNode.parentNode;
        if(checkBox.checked)
            currentRow.setAttribute("class","info");
        else
            currentRow.removeAttribute("class");
    },
    render: function () {
        return (
        <tr id={"row"+this.props.id}>
            <td>
                <input type="checkbox" id={"check"+this.props.id} className="checkBoxClass" onChange={this.OnCheckBoxChange}/>
            </td>
            <td>
                    { this.props.img !== undefined ? <img src={this.props.img} className="thumbnail" height="60px" width="75px"/> :
                        <img src={context + "/assets/images/datasources/Unknown.png"} className="thumbnail" height="60px" width="75px"/> }
            </td>

            <td>{this.props.name}</td>
            <td>{ this.props.location !== undefined ?
                <p>{this.props.location}</p> : "N/A" }</td>
            <td>{ this.props.alias !== undefined ? <p>{this.props.alias}</p> : "N/A" }</td>
            <td>{ this.props.gender !== undefined ? <p>{this.props.gender}</p> : "N/A" }</td>
            <td>{ this.props.occupation !== undefined ?
                <p>{this.props.occupation}</p> : "N/A" }</td>
            <td>{ this.props.birthday !== undefined ?
                <p>{this.props.birthday}</p> : "N/A" }</td>
            <td>{ this.props.country !== undefined ?
                <p>{this.props.country}</p> : "N/A" }</td>
            <td>{ this.props.label !== undefined ? <p>{this.props.label}</p> : "N/A" }</td>
            <td>{ this.props.webpage !== undefined ? <p><a href={this.props.webpage}
                                                                                            target="_blank"></a>
            </p> : "N/A" }</td>
            <td>{ this.props.active_email !== undefined ?
                <p>{this.props.active_email}</p> : "N/A" }</td>
            <td>{ this.props.wants !== undefined ?
                <p>{this.props.wants}</p> : "N/A" }</td>
            <td>{ this.props.haves !== undefined ?
                <p>{this.props.haves}</p> : "N/A" }</td>
            <td>{ this.props.top_haves !== undefined && this.props.top_haves !== "null" ?
                <p>{this.props.top_haves}</p> : "N/A" }</td>
            <td>{ this.props.interests !== undefined ?
                <p>{this.props.interests}</p> : "N/A" }</td>

            <span className="hidden" id={"desc" + this.props.id}>
                    <strong className="bold">Comment:</strong>
                    <br/>
                    <pre>{ this.props.comment !== undefined ? <p>{this.props.comment}</p> : "N/A" }</pre>
            </span>
        </tr>
        );
        }
        });
var ProductResultRow = React.createClass({
    render: function () {
        return (
            <tr>
                <td>
                    <input type="checkbox"/>
                </td>
                <td><img src={this.props.img} className="thumbnail" height="60px" width="75px"/></td>
                <td>{this.props.title}</td>
                <td>{ this.props.location !== undefined ? <p>{this.props.location}</p> : null }</td>
                <td>{ this.props.country !== undefined ? <p>{this.props.country}</p> : null }</td>
                <td>{ this.props.price !== undefined ? <p>{this.props.price}</p> : null }</td>
                <td>{ this.props.condition !== undefined ? <p>{this.props.condition}</p> : null }</td>
                <td>{ this.props.webpage !== undefined ? <a href={this.props.webpage} target="_blank"></a> : null }</td>
                <td><img src={context+"/assets/images/datasources/"+this.props.source+".png"} alt={"Information from "+this.props.source} height="45" width="45"/></td>
            </tr>
        );
    }
});


