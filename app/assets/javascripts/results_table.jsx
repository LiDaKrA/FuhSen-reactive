/**
 * Created by Ahmed on 9/24/2016.
 */
var ResultsTable = React.createClass({
    OnCheckBoxChange : function (id) {
        var checkBox = document.getElementById("check"+ id);
        var currentRow = checkBox.parentNode.parentNode;
        if(checkBox.checked)
            currentRow.setAttribute("class","info");
        else
            currentRow.removeAttribute("class");
    },
    getInitialState : function () {
      return {dialogID : "model_comment"};
    },
    render: function () {
        var resultsNodesSorted = this.props.data["@graph"].sort(compareRank);
        var resultsNodes = resultsNodesSorted.map(function (result,i) {
            var checkBoxHandle = this.OnCheckBoxChange.bind(this,i);
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
                        gender={result["fs:gender"]}
                        occupation={result["fs:occupation"]}
                        birthday={result["fs:birthday"]}
                        country={result["fs:country"]}
                        webpage={result.url}
                        active_email={result["fs:active_email"]}
                        wants={result["fs:wants"]}
                        haves={result["fs:haves"]}
                        top_haves={result["fs:top_haves"]}
                        interests={result["fs:interests"]}
                        liveInName = {result["fs:liveIn"]}
                        workedAtName = {result["fs:workAt"]}
                        studyAtName = {result["fs:studyAt"]}
                        OnCheckBoxChangeHandle = {checkBoxHandle}
                    >
                    </PersonResultRow>
                );
             } else if (result["@type"] === "foaf:Organization") {
                return (
                    <OrganizationResultRow
                        id = {i}
                        img={result.image}
                        name = {result["fs:title"]}
                        source={result["fs:source"]}
                        label={result["fs:label"]}
                        comment={result["fs:comment"]}
                        country={result["fs:country"]}
                        location={result["fs:location"]}
                        webpage={result.url}
                        OnCheckBoxChangeHandle = {checkBoxHandle}>
                    </OrganizationResultRow>
                );
            }
            else if (result["@type"] === "gr:ProductOrService") {
                return (
                    <ProductResultRow
                        id = {i}
                        img={result.image}
                        description = {result["fs:title"]}
                        source={result["fs:source"]}
                        location={result["fs:location"]}
                        country={result["fs:country"]}
                        price={result["fs:price"]}
                        condition={result["fs:condition"]}
                        webpage={result.url}
                        OnCheckBoxChangeHandle = {checkBoxHandle}>
                    </ProductResultRow>
                );
            }
            else if (result["@type"] === "foaf:Document") {
                 return (
                        <WebResultRow
                            id = {i}
                            img={context+"/assets/images/datasources/TorLogo.png"}
                            webpage={result.url}
                            comment={result["fs:excerpt"]}
                            source={result["fs:source"]}
                            OnCheckBoxChangeHandle = {checkBoxHandle}>
                        </WebResultRow>
                    );
            } else if (result["@type"] === "fs:Document") {
                 return (
                     <DocumentResultRow
                         id = {i}
                         img = {context+"/assets/images/icons/" + result["fs:filetype"] + ".png"}
                         label={result["fs:title"]}
                         comment={result["fs:comment"]}
                         webpage={result.url}
                         country={result["fs:country"]}
                         language={result["fs:language"]}
                         filename={result["fs:file_name"]}
                         extension={result["fs:filetype"]}
                         source={result["fs:source"]}
                         OnCheckBoxChangeHandle = {checkBoxHandle}>
                     </DocumentResultRow>
                 );
             }
        },this);

        return (
            <div className="table-responsive">
                <table className="table table-bordered table-hover">
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
                    <th>{getTranslation("link")}</th>
                    <th>{getTranslation("source")}</th>
                    <th>{getTranslation("nick")}</th>
                    <th>{getTranslation("location")}</th>
                    <th>Label</th>
                    <th>Comment</th>
                    <th>{getTranslation("gender")}</th>
                    <th>{getTranslation("occupation")}</th>
                    <th>{getTranslation("birthday")}</th>
                    <th>{getTranslation("country")}</th>
                    <th>{getTranslation("active_email")}</th>
                    <th>{getTranslation("liveIn")}</th>
                    <th>{getTranslation("workAt")}</th>
                    <th>{getTranslation("studyAt")}</th>
                </tr>

            );
        }
        else if(this.props.type == "product")
        {
            return(
                <tr>
                    <th>
                        <input type="checkbox" id="selectall" onClick={this.OnClickCheckBox}/>
                    </th>
                    <th></th>
                    <th>Description</th>
                    <th>{getTranslation("link")}</th>
                    <th>{getTranslation("location")}</th>
                    <th>{getTranslation("country")}</th>
                    <th>{getTranslation("price")}</th>
                    <th>{getTranslation("condition")}</th>
                    <th>{getTranslation("source")}</th>
                </tr>
            );
        }
        else if(this.props.type == "organization")
        {
            return(
                <tr>
                    <th>
                        <input type="checkbox" id="selectall" onClick={this.OnClickCheckBox}/>
                    </th>
                    <th></th>
                    <th>Name</th>
                    <th>{getTranslation("link")}</th>
                    <th>Label</th>
                    <th>Comment</th>
                    <th>{getTranslation("country")}</th>
                    <th>{getTranslation("location")}</th>
                    <th>{getTranslation("source")}</th>
                </tr>
            );
        }
        else if(this.props.type == "website")
        {
            return(
                <tr>
                    <th>
                        <input type="checkbox" id="selectall" onClick={this.OnClickCheckBox}/>
                    </th>
                    <th></th>
                    <th>Label</th>
                    <th>Comment</th>
                    <th>{getTranslation("link")}</th>
                    <th>{getTranslation("content")}</th>
                    <th>{getTranslation("title")}</th>
                    <th>Entity Type</th>
                    <th>Entity Name</th>
                    <th>{getTranslation("source")}</th>
                </tr>
            );
        }
        else //document
        {
            return(
                <tr>
                    <th>
                        <input type="checkbox" id="selectall" onClick={this.OnClickCheckBox}/>
                    </th>
                    <th></th>
                    <th>Label</th>
                    <th>Comment</th>
                    <th>{getTranslation("link")}</th>
                    <th>{getTranslation("country")}</th>
                    <th>{getTranslation("language")}</th>
                    <th>{getTranslation("filename")}</th>
                    <th>Extension</th>
                    <th>{getTranslation("source")}</th>
                </tr>
            );
        }
    }

});
var PersonResultRow = React.createClass({


    render: function () {
        return (
        <tr id={"row"+this.props.id}>
                <td>
                        <input type="checkbox" id={"check"+this.props.id} className="checkBoxClass" onChange={this.props.OnCheckBoxChangeHandle} />
                </td>
            <td>
                <div className="thumbnail">
                { this.props.img !== undefined ? <img src={this.props.img} height="60px" width="75px"/>:
                        <img src={context + "/assets/images/datasources/Unknown.png"} className="thumbnail" height="60px" width="75px"/> }
                </div>
            </td>

            <td>{this.props.name}</td>
            <td>{ this.props.webpage !== undefined ? <p><a href={this.props.webpage}

                                                           target="_blank"></a>
            </p> : "N/A" }</td>
            <td>
                <div class="thumbnail">
                    <img src={context+"/assets/images/datasources/"+this.props.source+".png"} alt={"Information from "+this.props.source} height="45" width="45"/>
                </div>
            </td>

            <td>{ this.props.alias !== undefined ? <p>{this.props.alias}</p> : "N/A" }</td>
            <td>{ this.props.location !== undefined ?
                <p>{this.props.location}</p> : "N/A" }</td>
            <td>{ this.props.label !== undefined ? <p>{this.props.label}</p> : "N/A" }</td>
            <td>{ this.props.comment !== undefined ? <RichText label="Comment" text={this.props.comment} maxLength={100}/> : "N/A"}</td>
            <td>{ this.props.gender !== undefined ? <p>{this.props.gender}</p> : "N/A" }</td>
            <td>{ this.props.occupation !== undefined ?
                <p>{this.props.occupation}</p> : "N/A" }</td>
            <td>{ this.props.birthday !== undefined ?
                <p>{this.props.birthday}</p> : "N/A" }</td>
            <td>{ this.props.country !== undefined ?
                <p>{this.props.country}</p> : "N/A" }</td>


            <td>{ this.props.active_email !== undefined ?
                <p>{this.props.active_email}</p> : "N/A" }</td>
            <td>{ this.props.liveInName !== undefined ?
                <p>{JSON.stringify(this.props.liveInName)}</p> : "N/A" }</td>
            <td>{ this.props.workedAtName !== undefined ?
                <p>{JSON.stringify(this.props.workedAtName)}</p> : "N/A" }</td>
            <td>{ this.props.studyAtName !== undefined ?
                <p>{JSON.stringify(this.props.studyAtName)}</p> : "N/A" }</td>

        </tr>
        );
        }
        });
var ProductResultRow = React.createClass({
    render: function () {
        return (
            <tr id={"row"+this.props.id}>
                <td>
                    <input type="checkbox" id={"check"+this.props.id} className="checkBoxClass" onChange={this.props.OnCheckBoxChangeHandle} />
                </td>
                <td><img src={this.props.img} className="thumbnail" height="60px" width="75px"/></td>
                <td>{this.props.description !== undefined ? <RichText label="Description" text={this.props.description} maxLength={100}/> : "N/A"}</td>
                <td>{ this.props.webpage !== undefined ? <a href={this.props.webpage} target="_blank"></a> : "N/A" }</td>
                <td>{ this.props.location !== undefined ? <p>{this.props.location}</p> : "N/A" }</td>
                <td>{ this.props.country !== undefined ? <p>{this.props.country}</p> : "N/A" }</td>
                <td>{ this.props.price !== undefined ? <p>{this.props.price}</p> : "N/A" }</td>
                <td>{ this.props.condition !== undefined ? <p>{this.props.condition}</p> : "N/A" }</td>
                <td>
                    <div class="thumbnail">
                        <img src={context+"/assets/images/datasources/"+this.props.source+".png"} alt={"Information from "+this.props.source} height="45" width="45"/>
                    </div>
                </td>
            </tr>
        );
    }
});

var OrganizationResultRow = React.createClass({
    render: function () {
        return (
            <tr id={"row"+this.props.id}>
                <td>
                    <input type="checkbox" id={"check"+this.props.id} className="checkBoxClass" onChange={this.props.OnCheckBoxChangeHandle} />
                </td>
                <td>
                    <img src={this.props.img} className="thumbnail" height="60px" width="75px"/></td>
                <td>{this.props.name}</td>
                <td>{ this.props.webpage !== undefined ? <a href={this.props.webpage} target="_blank"></a> : "N/A" }</td>
                <td>{ this.props.label !== undefined ? <p>{this.props.label}</p> : "N/A" }</td>
                <td>{ this.props.comment !== undefined ? <RichText label="Comment" text={this.props.comment} maxLength={100}/> : "N/A"}</td>
                <td>{ this.props.country !== undefined ? <p>{this.props.country}</p> : "N/A" }</td>
                <td>{ this.props.location !== undefined ? <p>{this.props.location}</p> : "N/A" }</td>
                <td>
                    <div class="thumbnail">
                        <img src={context+"/assets/images/datasources/"+this.props.source+".png"} alt={"Information from "+this.props.source} height="45" width="45"/>
                    </div>
                </td>
            </tr>
        );
    }
});
var WebsiteResultRow = React.createClass({
    render: function () {
        return (
            <tr id={"row"+this.props.id}>
                <td>
                    <input type="checkbox" id={"check"+this.props.id} className="checkBoxClass" onChange={this.props.OnCheckBoxChangeHandle} />
                </td>
                <td><img src={this.props.img} className="thumbnail" height="60px" width="75px"/></td>
                <td>{ this.props.label !== undefined ? <p>{this.props.label}</p> : "N/A" }</td>
                <td>{ this.props.comment !== undefined ? <RichText label="Comment" text={this.props.comment} maxLength={100}/> : "N/A"}</td>
                <td>{ this.props.webpage !== undefined ? <a href={this.props.webpage} target="_blank"></a> : "N/A" }</td>
                <td>{ this.props.content !== undefined ? <p>{this.props.content}</p> : "N/A" }</td>
                <td>{ this.props.title !== undefined ? <p>{this.props.title}</p> : "N/A" }</td>
                <td>{ this.props.entity_type !== undefined ? <p>{this.props.entity_type}</p> : "N/A" }</td>
                <td>{ this.props.entity_name !== undefined ? <p>{this.props.entity_name}</p> : "N/A" }</td>
                <td>
                    <div class="thumbnail">
                        <img src={context+"/assets/images/datasources/"+this.props.source+".png"} alt={"Information from "+this.props.source} height="45" width="45"/>
                    </div>
                </td>
            </tr>
        );
    }
});
var DocumentResultRow = React.createClass({
    render: function () {
        return (
            <tr id={"row"+this.props.id}>
                <td>
                    <input type="checkbox" id={"check"+this.props.id} className="checkBoxClass" onChange={this.props.OnCheckBoxChangeHandle} />
                </td>
                <td><img src={this.props.img} className="thumbnail" height="60px" width="75px"/></td>
                <td>{ this.props.label !== undefined ? <p>{this.props.label}</p> : "N/A" }</td>
                <td>{ this.props.comment !== undefined ? <RichText label="Comment" dialogID = {"model_comment" + this.props.id} text={this.props.comment} maxLength={100}/> : null}</td>
                <td>{ this.props.webpage !== undefined ? <a href={this.props.webpage} target="_blank"></a> : "N/A" }</td>
                <td>{ this.props.country !== undefined ? <p>{this.props.country}</p> : "N/A" }</td>
                <td>{ this.props.language !== undefined ? <p>{this.props.language}</p> : "N/A" }</td>
                <td>{ this.props.file_name !== undefined ? <p>{this.props.file_name}</p> : "N/A" }</td>
                <td>{ this.props.extension !== undefined ? <p>{this.props.extension}</p> : "N/A" }</td>
                <td>
                    <div class="thumbnail">
                        <img src={context+"/assets/images/datasources/"+this.props.source+".png"} alt={"Information from "+this.props.source} height="45" width="45"/>
                    </div>
                </td>
            </tr>
        );
    }
});
