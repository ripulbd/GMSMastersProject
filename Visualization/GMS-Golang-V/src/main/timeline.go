/*
===== Installation Guide =====
Plugins:
	1) mgo for connect with mongodb (https://labix.org/mgo)
	> go get gopkg.in/mgo.v2
	   
	2) Gorilla for session (http://www.gorillatoolkit.org/pkg/sessions) 
	> go get github.com/gorilla/sessions
	
	3) Snowball steming algorithm (https://github.com/kljensen/snowball)
	> go get github.com/kljensen/snowball
	   
Mongodb:
	1) Start Server Command (Run everytime)
	> mongod --dbpath "%DBPATH%"
	
	2) Remove database (require server on)
	> mongo
	> use %DB%
	> db.%COLLECTION.drop()
	
	3) Install database (require server on)
	> mongoimport --db %DB% --collection %COLLECTION% --file %FILENAME%
	
	4) Check current collections
	> mongo
	> use %DB%
	> show collections

Appendix:
	%DBPATH% = path to storage your database
	%DB% = database name, DB_NAME in constant
	%COLLECTION% = collection name, DB_COLLECTION_* in constant
		This program has 3 collections
		1) record = record of all news, DB_COLLECTION_RECORD in constant
		2) modeling = modeling news with keywords, DB_COLLECTION_NEWS
		3) keyword = keywords information (id,name,path), DB_COLLECTION_KEYWORD
	%FILENAME% = json file name including path
	- You can change XML file name at FILE_XML_NAME in constant
	- You should drop old collection if you want to update the new json file
	
	
=== About timeline.go ===
   This program is server-side application to show topics and keyword from Topic Modeling
   This program uses topic as menubar and subtopic ans submenubar.
   This program uses keyword as tag cloud.
   This program classifies similar news into the same group by using cosine similarity method
   Cosine similarity in this program is use keywords and title of each news as input
   This program display the similar news into the same group and show summary of similar news grom Summarization Server
   This program show individual news details in the new tab
   
=== How to use ===
  1) make sure you have ontology.xml in the correct location
  2) install database and have database and collection name as same as constant field
  3) start mongoDB server
  4) start Summarization server
  5) Run timeline.go
  6) go to localhost:8090 in your browser
  *** more details in ReadMe.md
*/
package main

import (
	"fmt"
    "gopkg.in/mgo.v2"
    "gopkg.in/mgo.v2/bson"
	"flag"
	"encoding/xml"
	"encoding/json"
	"encoding/gob"
	"io"
	"os"
	"path/filepath"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"regexp"
	"html/template"
	"github.com/gorilla/sessions"
	"strings"
	"github.com/gorilla/context"
	"math"
	"github.com/kljensen/snowball"
)

const (
	SESSION_NAME_TOPIC_HANDLER = "TopicHandler"
	SESSION_KEY_PREVIOUS_TOPIC = "PreviousTopic"
	SESSION_KEY_TOPIC_PATH = "TopicPath"
	PATH_SEPARATER = "/";
	DB_NAME = "demo";
	DB_COLLECTION_RECORD = "records";
	DB_COLLECTION_NEWS = "modeling";
	DB_COLLECTION_KEYWORD = "keywords";
	COSINE_THRESHOLD = 0.4;
	FILE_XML_NAME = "ontology.xml";
)

var (
	addr = flag.Bool("addr", false, "find open address and print to final-port.txt")
	store = sessions.NewCookieStore([]byte("something-very-secret"))
)

type Topic struct{
	Name 		string 			`xml:"name,attr"` 
	SubTopics	[]Topic 		`xml:"topic"`
	Keywords	[]Keyword		`xml:"keyword"`
	ParentName	string
	Path		string			// Path is not include themself
	/* 
	 * Path format is Parent1Name/Parent2Name/.../ParentName
	 * '/' is based on PATH_SEPARATER in constant
	 */
}

type Keyword struct{
	ID			string			`bson:"id"`
	Name 		string 			`xml:"name,attr" bson:"keyword"`
	Path		string			`bson:"path"`
	Weight		int 
}

type Image struct {
	Name    	string 			`bson:"name"`
	Caption 	string			`bson:"caption"`
}

type News struct{
	NewsID		string			`bson:"newsId"`
	Title       string    		`bson:"title"`
	Description string    		`bson:"description"`
	TimeStamp   string    		`bson:"timeStamp"`
	Category    string    		`bson:"category"`
	Url         string    		`bson:"url"`
	Source      string    		`bson:"source"`
	MainStory   string   		`bson:"mainStory"`
	Images      []Image  		`bson:"images"`
	KeywordIDs	[]string		`bson:"keywords"`
	Comments    []Comment 		`bson:"comments"`		
}

type Comment struct {
	UserName    string    		`bson:"userName"`
	TimeStamp   string    		`bson:"timeStamp"`
	CommentBody string    		`bson:"commentBody"`
	UpVote      int       		`bson:"upVote"`
	DownVote    int       		`bson:"downVote"`
	Replies     []Comment 		`bson:"replies"`
}


type NewsGroup struct{
	Headline	string
	Summary		string
	News		[]News
}

type ListNewsGroup struct{
	NewGroups	[]NewsGroup
	Keyword		Keyword
}

func init() {
    gob.Register(&Topic{})
    gob.Register(&Keyword{})
    
    store.Options = &sessions.Options{
	    Path:     "/",
	    MaxAge:   86400,
	    HttpOnly: true,
	}
}

// generate value of individual vector from the template
func generateVectorValue(template,keywords []string) ([]int){
	var vector = make([]int,len(template))
	
	for _,keyword := range keywords {	
		for i := range template {	
			if template[i] == keyword {
				vector[i]++
				break
			}
		}
	}
	return vector
}

/* Classification the news into the group, each group contains the similar news by using cosine similarity method 
 * Use one keyword/string as 1 value for example keyword = {"1","2","3"} -> template("1","2","3") = (1,1,1)
 * Steps:
 *  1) Find template vector or all possible keywords and stemmed string from title
 *  Ex. news1.keywords = {"1","2","3"} and news2.keywords = {"1","3","4"}
 *  Template vector should be equal {"1","2","3","4"}
 *  2) find vector value for every news
 *  Ex. vector_news1 = (1,1,1,0) and vector_news2 = (1,0,1,1)
 *  3) calculate cosine similarity to all possible pairs (O(n(n-1)/2)
 *  Ex. Input = news1, news2, news3, news4
 *     n = 4, times = 4(4-1)/2 = 6
 *     calculate these pairs:
 *                   news1-news2, news1-news3, news1-news4
 *                   news2-news3, news2-news4, news3-news4
 *  4) Evaluate groups with threshold
 *     Example result: news1-news2 = 0.75, news1-news3 = 0.56, news1-news4 = 0.86
 *                     news2-news3 = 0.82, news2-news4 = 0.45, news3-news4 = 0.90
 *             Threshold = 0.70
 *     constrains: to avoid conflict
 *     4.1) classify by the order; if each news item was already arranged in the group, the pairs which begin with that news will be skipped.
 *         Ex. the cosine similarity value of news2-news3, news2-news4 will be ignored because news2 was already classified. 
 *     4.2) if cosine similarity value is more than threshold but the opposite news was already occupied. 
 *          We will compare cosine similarity between current group and new group and move it to the group that has the greater value.
 *         Ex. news4 already in the same group with news1 but the value between news3 and new4 are more than 
 *             the value between news1 and new4 then, news4 will move to be in the same group with news2 instead.
 *  5) Groups similar news together, create NewsGroup and return
 * http://www.gettingcirrius.com/2010/12/calculating-similarity-part-1-cosine.html
 */
func generateNewsGroup(allNews []News) ([]NewsGroup){
	// find vector template from intersection of every keyword
	var vectorTemplate1 []string // vector template for keywords
	var vectorTemplate2 []string // vector template for title
	var contains bool
	var titleWords [][]string = make([][]string,len(allNews))// array to store every title from each news
	i := 0
	for _,news := range allNews {	
		// find vector template of keywords
		for _,keyword := range news.KeywordIDs {	
			contains = false
			for _,v := range vectorTemplate1 {	
				if v == keyword {
					contains = true
					break
				}
			}
			if contains {
				// already add keyword into vector
				continue
			}
			vectorTemplate1 = append(vectorTemplate1,keyword)
		}
		
		titleWords[i] = make([]string,0)
		// find vector template for title
		for _,title := range strings.Split(news.Title," ") {	
			contains = false
			// do stem
			stemmed, err := snowball.Stem(title, "english", true)
		    if err == nil{
		        title = stemmed
		    }
		    titleWords[i] = append(titleWords[i],title)
			for _,v := range vectorTemplate2 {	
				if v == title {
					contains = true
					break
				}
			}
			if contains {
				// already add keyword into vector
				continue
			}
			vectorTemplate2 = append(vectorTemplate2,title)
		}
		i++
	}
	
	var allVectorValue1 [][]int = make([][]int,len(allNews)) // all vector value for keywords
	var allVectorValue2 [][]int = make([][]int,len(allNews)) // all vector value for title
	
	// find vector value
	for i := range allNews {	
		allVectorValue1[i] = generateVectorValue(vectorTemplate1,allNews[i].KeywordIDs)
		allVectorValue2[i] = generateVectorValue(vectorTemplate2,titleWords[i])
	}
	
	// calculate consine similarity
	var calculateResult1 [][]float64 = make([][]float64,len(allNews)) // calculate result for keywords
	var calculateResult2 [][]float64 = make([][]float64,len(allNews)) // calculate result for title
	for i := range calculateResult1 {
		calculateResult1[i] = make([]float64,len(allNews))
		calculateResult2[i] = make([]float64,len(allNews))
	}

	for i := 0; i<len(allVectorValue1) ; i++ {	
		for j := i+1; j<len(allVectorValue1) ; j++ {	
			calculateResult1[i][j] = calculateCosineSimilarity(allVectorValue1[i],allVectorValue1[j])
			calculateResult2[i][j] = calculateCosineSimilarity(allVectorValue2[i],allVectorValue2[j])
		}
	}
	
	// group[i] = j <-> news j is in group i
	var group []int = make([]int,len(allNews))
	// set default value, news i is in group i
	for i := 0; i<len(group) ; i++ {	
		group[i] = i;
	}
	for i := 0; i<len(allVectorValue1) ; i++ {	
		if group[i] != i {
			/* 
			 * if news i already move a group, it assume that pair of similar which news i 
			 * should be in the same group with news i
			 */
			continue
		}
		for j := i+1; j<len(allVectorValue1) ; j++ {
			if calculateResult1[i][j]*calculateResult2[i][j] > COSINE_THRESHOLD {
				if group[j] != j && calculateResult1[i][j]*calculateResult2[i][j] < calculateResult1[group[j]][j]*calculateResult2[group[j]][j]{
					/* 
					 * same reason as above
					 */
					continue
				}
				fmt.Println("group: ",i," index: ",j," p(k) = ",calculateResult1[i][j]," p(t) = ",calculateResult2[i][j])
				group[j] = i
			}
		}
	}
	
	// find number of group
	var number []int
	var duplicate bool
	for i := 0; i<len(group) ; i++ {	
		duplicate = false
		for j := 0; j<len(number) ; j++ {
			if group[i] == number[j] {
				duplicate = true
				break
			}
		}
		if duplicate {
			continue
		}
		number = append(number,group[i])
	}

	// create group result
	var result []NewsGroup = make([]NewsGroup,len(number))
	for i := 0; i<len(number) ; i++ {
		fmt.Println("<group>",number[i])	
		var news []News
		for j := 0; j<len(group) ; j++ {
			if group[j] == number[i] {
				news = append(news,allNews[j])
				fmt.Println("index:",j,allNews[j].Title)
			}
		}
		//result[i] = NewsGroup{"","",news}	
		
		if len(news) > 1 {
			//check duplicate news
			for k := 0; k<len(news);k++ {
				for q := k+1; q<len(news);q++ {
					if news[k].Url == news[q].Url {
						//delete duplicate news
						news[q],news = news[len(news)-1], news[:len(news)-1]
						q--
					}
				}
			}
			
			// sorting more news -> less news
			for k := 0; k<=i;k++ {
				if len(result[k].News)<len(news) {
					copy(result[k+1:],result[k:])
					result[k] = NewsGroup{"","",news}	
					break
				}
			}
		}else {
			result[i] = NewsGroup{"","",news}	
		}
	} 
	
	return result
}

/* calculate cosine similarity value between vector1 and vector2 
 * example: vector1 = (1, 1, 1, 2, 0)    vector2 = (1, 1, 1, 2, 1)
 * We can now perform the cosine similarity calculation:
 *   1) Dot Product = (1 * 1) + (1 * 1) + (1 * 1) + (2 * 2) + (0 * 1) = 1 + 1 + 1 + 4 + 0 = 7
 *   2) Magnitude of apple = √(12 + 12 + 12 + 22 + 02) = √(1 + 1 + 1 + 4 + 0) = √(7)
 *   3) Magnitude of applet = √(12 + 12 + 12 + 22 + 12) = √(1 + 1 + 1 + 4 + 1) = √(8)
 *   4) Products of magnitudes A & B =  √(7) * √(8) = √(56) = 7.48331477
 *   5) Divide the dot product of A & B by the product of magnitude of A & B = 7 / 7.48331477 = .935414347 (or about 94% similar).
 * http://www.gettingcirrius.com/2010/12/calculating-similarity-part-1-cosine.html
 */
func calculateCosineSimilarity(vector1,vector2 []int)(float64){
	// find dot product between vector 1 and vector 2
	var dotProduct int = 0
	for i:=0; i< len(vector1); i++ {
		dotProduct += vector1[i]*vector2[i]
	}
	
	// find product of magnitudes between vector 1 and vector 2
	var sumSquare1, sumSquare2, productMagnitudes float64 
	sumSquare1 = 0
	for i:=0; i< len(vector1); i++ {
		sumSquare1 += float64(vector1[i]*vector1[i])
	}
	sumSquare2 = 0
	for i:=0; i< len(vector2); i++ {
		sumSquare2 += float64(vector2[i]*vector2[i])
	}
	productMagnitudes = math.Sqrt(sumSquare1*sumSquare2)
	
	return float64(dotProduct) / productMagnitudes
}

/* read whole ontology.xml file and convert into Topic syntax */
func readTopic(reader io.Reader) (Topic, error) {
   
    decoder := xml.NewDecoder(reader)
    var topic Topic
	var inElement string
    for { 
	  	t,_ := decoder.Token()
	   	if t == nil { 
        	break 
   		} 
	   	switch se := t.(type) { 
    	case xml.StartElement:
    		inElement = se.Name.Local
			if inElement =="topic"{
    			//Praser will finish here in once time.
    			decoder.DecodeElement(&topic, &se)
//    			fmt.Println(topic)
    		}
    	}
    }

    return topic	, nil
}

/* return the Topic of XML file */
func readXML()(Topic){
	xmlPath, err := filepath.Abs(FILE_XML_NAME)
    if err != nil {
        fmt.Println(err)
        os.Exit(1)
    }

    file, err := os.Open(xmlPath)
    if err != nil {
        fmt.Println(err)
        os.Exit(1)
    }

    defer file.Close()


    topic, err := readTopic(file)
    if err != nil {
        fmt.Println(err)
        os.Exit(1)
    }
    
    return topic
}

/* return specific topic by name and path */
func readTopicNameXML(name string, path string)(Topic){
	  topic := readXML()
	  topic = topicTraveller(topic,name,path)
	  return topic
}

// Depth first search
/*
	search the specific topic by name and path
	If path is empty String, assume that user clicked on High Level topic and path was unknown.
*/
func topicTraveller(topic Topic,name string, path string)(Topic){
	var  result Topic
	if topic.Name != name {
		for i := range topic.SubTopics {
			t :=  &topic.SubTopics[i]
    		t.ParentName = topic.Name
    		// set path
    		if topic.Path != "" {
    			t.Path =  topic.Path + PATH_SEPARATER + topic.Name	
    		}else {
    			t.Path =  topic.Name
    		}
    		result = topicTraveller(*t,name,path)
    		if result.Name == name && (path == "" || result.Path == path) {
    			break		
    		}
    	}
    }else{
    	result = topic
    }
	return result
}

/* Send High-level topic to display as menubar */
func timelineHandler(w http.ResponseWriter, r *http.Request) {
	topic := readXML();

	renderTemplate(w, "timeline", &topic)
}

/* show the submenu bar and tag cloud*/
func topicHandler(w http.ResponseWriter, r *http.Request){
	
	var topic Topic
	var path string = ""

    //take the tagname from the url	and print it
	tagname := r.URL.Query()["tagname"][0];
	fmt.Printf("Query: %s\n", tagname)
	
	session, _ := store.Get(r, SESSION_NAME_TOPIC_HANDLER)
	// get the previous topic from session
	if previous_topic, ok := session.Values[SESSION_KEY_PREVIOUS_TOPIC].(*Topic); ok {
		if previous_topic.ParentName == tagname {
			//previous button was selected
			path = strings.Split(previous_topic.Path,PATH_SEPARATER+tagname)[0]
		}else{
			for _,t := range previous_topic.SubTopics {	
		    	if t.Name == tagname {	
		    		// sub topic was selected
		    		t.ParentName = previous_topic.Name;
		    		t.Path = previous_topic.Path + PATH_SEPARATER + previous_topic.Name
		    		topic = t;
		    		break	
		   	    }
		 	}
		}
		
	}
	
	// new High level topic or previous button was selected
	if topic.Name == "" {
		//find the specific topic in XML which has equal name with tagname
		topic = readTopicNameXML(tagname,path);	
	}
	
	// save topic without keywords to session because keywords are too big
	fmt.Println(topic);
	session.Values[SESSION_KEY_PREVIOUS_TOPIC] = &topic
	session.Save(r,w)
	
	dbSession, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer dbSession.Close()

	// Optional. Switch the session to a monotonic behavior.
	dbSession.SetMode(mgo.Monotonic, true)
	
	// get all keywords that belong to selected topic
	collectionKeywords := dbSession.DB(DB_NAME).C(DB_COLLECTION_KEYWORD)
	collectionKeywords.Find(bson.M{"path":topic.Path + PATH_SEPARATER + topic.Name}).All(&topic.Keywords)
	
	//update weight
	for i := range topic.Keywords {
		var news []News
		// get all news that has keywords[i] to calculate weight
		collectionNews := dbSession.DB(DB_NAME).C(DB_COLLECTION_NEWS)
		collectionNews.Find(bson.M{"keywords":topic.Keywords[i].ID}).All(&news)
		k :=  &topic.Keywords[i]
		k.Weight = len(news)
	}
	
	//send it back to html
	js, err := json.Marshal(topic)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
		
	w.Header().Set("Content-Type", "application/json")
  	w.Write(js)
}

/* show the list groups of similar news*/
func showListHandler(w http.ResponseWriter, r *http.Request) {
	
	var keyword Keyword
	keyword.Name = r.URL.Query()["keyword"][0]
	fmt.Println("Hello",keyword)
	
	session, _ := store.Get(r, SESSION_NAME_TOPIC_HANDLER)
	
	// get keyword path from current topic in session
	if previous_topic, ok := session.Values[SESSION_KEY_PREVIOUS_TOPIC].(*Topic); ok {
		keyword.Path = previous_topic.Path + PATH_SEPARATER + previous_topic.Name
	}else{
		// error
		return
	}
	
	dbSession, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer dbSession.Close()
	
	fmt.Println(keyword)

	// Optional. Switch the session to a monotonic behavior.
	dbSession.SetMode(mgo.Monotonic, true)
	
	// get selected keyword details
	collectionKeywords := dbSession.DB(DB_NAME).C(DB_COLLECTION_KEYWORD)
	collectionKeywords.Find(bson.M{"keyword":keyword.Name,"path":keyword.Path}).One(&keyword)
	
	var news []News
	// get all news that has selected keyword
	collectionNews := dbSession.DB(DB_NAME).C(DB_COLLECTION_NEWS)
	collectionNews.Find(bson.M{"keywords":keyword.ID}).All(&news)
	
	collectionRecords := dbSession.DB(DB_NAME).C(DB_COLLECTION_RECORD)
	for i := range news {
		var n News
		// update news details from records database
		collectionRecords.Find(bson.M{"_id":bson.ObjectIdHex(news[i].NewsID)}).One(&n)
		n.NewsID = news[i].NewsID
		n.KeywordIDs = news[i].KeywordIDs
		news[i] = n 
	}
	
	fmt.Println("input",len(news))
	var newsGroup []NewsGroup = generateNewsGroup(news)
	fmt.Println("output",len(newsGroup))
	var newsList string
	// generate summary for each group
	for i := range newsGroup {
		newsList = ""
		for _,news := range newsGroup[i].News {
			if newsList == "" {
				newsList = news.NewsID
			}else{
				newsList = newsList+";"+news.NewsID
			}
		}	
		fmt.Println(newsList)
		// connect to Summarization Server + send newsID to server
		response, err := http.Get( "http://127.0.0.1:8080/Summary/servlet/server?newslist="+newsList)
		if err != nil {
	        fmt.Printf("%s", err)
	    } else {
	        defer response.Body.Close()
	        contents, err := ioutil.ReadAll(response.Body)
	        if err != nil {
	            fmt.Printf("%s", err)
	            os.Exit(1)
	        }
	        // update summary of each group
	        group := &newsGroup[i]
	        group.Summary =  string(contents)
	    }
	}
	
	var listGroup ListNewsGroup = ListNewsGroup{newsGroup,keyword}
	js, err := json.Marshal(listGroup)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
		
	w.Header().Set("Content-Type", "application/json")
  	w.Write(js)
}

/* show individual news page */
func indiNewsHandler(w http.ResponseWriter, r *http.Request) {
	
	url := r.URL.Query()["url"][0];
	
	fmt.Printf("Query: %s\n", url)
	
	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()

	// Optional. Switch the session to a monotonic behavior.
	session.SetMode(mgo.Monotonic, true)

	c := session.DB(DB_NAME).C(DB_COLLECTION_RECORD)

	var result News
	err = c.Find(bson.M{"url": url}).One(&result)
	
	if err != nil {
		log.Fatal(err)
	}

	renderDetailNews(w, "detailNews", &result)
}

func renderDetailNews(w http.ResponseWriter, tmpl string, p *News) {
	// Execute the template for each recipient.
	err := getTemplate("detailNews.html").ExecuteTemplate(w, tmpl+".html", p)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}


var funcMap = template.FuncMap{
        // The name "inc" is what the function will be called in the template text.
        "inc": func(i int) int {
            return i + 1
        },
}

func getTemplate(html string) (*template.Template){
	return template.Must(template.New("test").Funcs(funcMap).ParseFiles(html))
}


func renderTemplate(w http.ResponseWriter, tmpl string, p *Topic) {
	// Execute the template for each recipient.
	err := getTemplate("timeline.html").ExecuteTemplate(w, tmpl+".html", p)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}

}



var validPath = regexp.MustCompile("^/(index|save|view)$")

func makeHandler(fn func(http.ResponseWriter, *http.Request)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		fn(w, r)
	}
}

func main() {
	flag.Parse()
	http.HandleFunc("/", makeHandler(timelineHandler))
	http.HandleFunc("/timeline/", makeHandler(timelineHandler))
	http.HandleFunc("/showlist", makeHandler(showListHandler))
	http.HandleFunc("/subtags", makeHandler(topicHandler))
	http.HandleFunc("/indiNews", makeHandler(indiNewsHandler))
	http.Handle("/resources/", http.StripPrefix("/resources/", http.FileServer(http.Dir("resources"))))

    if *addr {
		l, err := net.Listen("tcp", "127.0.0.1:0")
		if err != nil {
			log.Fatal(err)
		}
		err = ioutil.WriteFile("final-port.txt", []byte(l.Addr().String()), 0644)
		if err != nil {
			log.Fatal(err)
		}
		s := &http.Server{}
		s.Serve(l)
		return
	}

	http.ListenAndServe(":8090",  context.ClearHandler(http.DefaultServeMux))
}

