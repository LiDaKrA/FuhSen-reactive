/**
 * Created by Daniel Brilz on 03.02.2016.
 */
var dictEng = {
    "resultsfor": "Results for",
    "age": "Age",
    "nationality":"Nationality",
    "name":"Name",
    "address":"Address",
    "gender":"Gender",
    "mail":"Mail",
    "url":"url",
    "people": "People",
    "organisations":"Organisations",
    "items":"Items",
    "submit":"Submit",
    "yoursearch":"Persons, Organizations or Products",
    "outof":"out of",
    "results":"results",
    "scrolldown":"Scroll down to see more results",
    "noeligibleresults":"No eligible results!",
    "adjustfilters":"Adjust filters or wait for incoming results.",
    "activate":"activate",
    "filterbyvalue":"filter by value",
    'german':"German",
    'ghanaian':"Ghanaian",
    'greek':"Greek",
    'icelander':"Icelander",
    'indian':"Indian",
    'indonesian':"Indonesian",
    'polish':"Polish",
    'portuguese':"Portuguese",
    'swedish':"Swedish",
    'swiss':"Swiss",
    'dutch':"Dutch",
    'australian':"Australian",
    'austrian':"Austrian",
    'czech':"Czech",
    'danish':"Danish",
    "female":"Female",
    "male":"Male",
    "toolactive":"ACTIVATE: Shows results that contain this Property (ignoring its value).",
    "tooltype":"BY VALUE: Filters all results that own the entered value.",
    "toolmostfreq":"LISTING: Shows the most frequent values of this property. You may filter by selecting them.",
    "toolminmax":"MIN-MAX: Enables a range filter by defining a lower limit and an upper limit.",
    "newfbtoken":"Retrieve a new access token",
    "novalidfbtkfound":"No valid Facebook token found, if Facebook results are needed please:",
    "hours":"hours",
    "days":"days",
    "validfbtkfound":"Facebook token found, still valid for ",
    "products":"Products",
    "birthday":"Birthday",
    "exporttocsv":"Export to CSV",
    "bittewarten":"Searching, please wait ...",
    "nick":"Nick",
    "location":"Location",
    "webpage":"Webpage",
    "link":"Link",
    "comment":"Comment",
    "resultfilters":"Results filters",
    "occupation":"Occupation",
    "livein":"Location",
    "workfor":"Work",
    "studies":"Studies",
    "page":"Page",
    "sortedby":"Sorted by frequency",
    "clickhere":"Click here to visit to this webpage.",
    "checkingfbtoken": "Checking Facebook token ...",
    "country": "Country",
    "price": "Price",
    "condition": "Condition"
};
var dictGer = {
    "resultsfor": "Ergebnisse für",
    "age": "Alter",
    "nationality":"Nationalität",
    "name":"Name",
    "address":"Adresse",
    "gender":"Geschlecht",
    "mail":"E-Mail",
    "url":"Url",
    "people": "Personen",
    "organisations":"Organisationen",
    "items":"Gegenstände",
    "submit":"Absenden",
    "yoursearch":"Person, Organisation oder Produkt",
    "outof":"von",
    "results":"Ergebnisse",
    "scrolldown":"Scrollen Sie bis nach unten um weitere Ergebnisse anzuzeigen",
    "noeligibleresults": "Keine qualifizierten Ergebnisse!",
    "adjustfilters":"Passen Sie die Filter an oder warten Sie auf eingehende Ergebnisse.",
    "activate":"aktivieren",
    "filterbyvalue":"Nach einem Wert filtern",
    "forename":"Vorname",
    "surname":"Nachname",
    "street":"Straße",
    "source":"Quelle",
    'german':"Deutsch",
    'ghanaian':"Ghanaisch",
    'greek':"Griechisch",
    'icelander':"Isländisch",
    'indian':"Indisch",
    'indonesian':"Indonesisch",
    'polish':"Polnisch",
    'portuguese':"Portugisisch",
    'swedish':"Schwedisch",
    'swiss':"Schweitzerisch",
    'dutch':"Niederländisch",
    'australian':"Australisch",
    'austrian':"Österreichisch",
    'czech':"Tschechisch",
    'danish':"Dänisch",
    "female":"Weiblich",
    "male":"Männlich",
    "brasilian":"Brasilianisch",
    "iranian":"Iranisch",
    "kroatian":"Kroatisch",
    "spanish":"Spanisch",
    "russian":"Russisch",
    "america":"Amerikanisch",
    "english":"Englisch",
    "japanese":"Japanisch",
    "hungarian":"Ungarisch",
    "norwegian":"Norwegisch",
    "toolactive":"AKTIVIEREN: Zeigt alle Ergebnisse an, die diese Eigenschaft besitzen (unabhängig von deren Wert).",
    "tooltype":"NACH WERT: Filtert alle Ergebnisse nach dem eingegebenen Begriff.",
    "toolmostfreq":"AUFLISTUNG: Zeigt die häufgisten Werte dieser Eigenschaft und ermöglicht eine Filterung danach.",
    "toolminmax":"MIN-MAX: Ermöglich die Eingabe einer Unter- oder Obergrenze für diese Eigenschaft.",
    "newfbtoken":"Ein neues FB Access Token bekommen",
    "novalidfbtkfound":"Keine valid FB oken gefunden. Wenn Sie FB ergebnissen nutzen wollen, bitte:",
    "hours":"Stunden",
    "days":"Tagen",
    "validfbtkfound":"Facebook token gefunden, noch valid für ",
    "products":"Produkte",
    "birthday":"Geburstag",
    "exporttocsv":"Exportieren als CSV",
    "bittewarten":"Bitte warten Sie, während die Ergebnisse laden...",
    "nick":"Spitzname",
    "location":"Ort",
    "webpage":"Website",
    "link":"Link",
    "comment":"Kommentar",
    "resultfilters":"Ergebnisse filtern",
    "occupation":"Beruf",
    "livein":"Lebt in",
    "workfor":"Arbeitet bei",
    "studies":"Studium an",
    "page":"Seite",
    "sortedby":"Sortieren nach Frequenz",
    "clickhere":"Clicken Sie hier um diese WebSite zu besuchen.",
    "checkingfbtoken": "Überprüfen Facebook Token ...",
    "country": "Land",
    "price": "Preis",
    "condition": "Bedingung"
};

window.dictGer = dictGer;
window.dictEng = dictEng;

//if(window.globalDict === undefined){
if(window.localStorage.getItem("lang") === undefined || window.localStorage.getItem("lang") === "ger"){
    window.globalDict = dictGer;
    window.localStorage.lang = "ger";
}
else{
    window.globalDict = dictEng;
    window.localStorage.lang = "eng";
}

function getTranslation(toTranslate){
    if(toTranslate in window.globalDict){
        return window.globalDict[toTranslate];
    }
    else return toTranslate;
}

function checkLanguage(){
    if(window.localStorage.getItem("lang") === undefined) {
        window.globalDict = dictGer;
        window.localStorage.lang = "ger";
    }
    else if(window.localStorage.getItem("lang") === "ger") {
        window.globalDict = dictGer;
    }
    else if(window.localStorage.getItem("lang") === "eng") {
        window.globalDict = dictEng;
    }
}

window.getTranslation = getTranslation;
window.checkLanguage = checkLanguage;

