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
	FILE_XML_NAME = "xmlDemo.xml";
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

func readTopicNameXML(name string, path string)(Topic){
	  topic := readXML()
	  topic = topicTraveller(topic,name,path)
	  return topic
}

// Depth first search
/*
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

func timelineHandler(w http.ResponseWriter, r *http.Request) {
	topic := readXML();

	renderTemplate(w, "timeline", &topic)
}

func topicHandler(w http.ResponseWriter, r *http.Request){
	
	var topic Topic
	var path string = ""

    //take the tagname from the url	and print it
	tagname := r.URL.Query()["tagname"][0];
	fmt.Printf("Query: %s\n", tagname)
	
	session, _ := store.Get(r, SESSION_NAME_TOPIC_HANDLER)
	
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

	collectionKeywords := dbSession.DB(DB_NAME).C(DB_COLLECTION_KEYWORD)
	collectionKeywords.Find(bson.M{"path":topic.Path + PATH_SEPARATER + topic.Name}).All(&topic.Keywords)
	
	//update weight
	for i := range topic.Keywords {
		var news []News
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

func showListHandler(w http.ResponseWriter, r *http.Request) {
	
	var keyword Keyword
	keyword.Name = r.URL.Query()["keyword"][0]
	fmt.Println("Hello",keyword)
	
	session, _ := store.Get(r, SESSION_NAME_TOPIC_HANDLER)
	
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

	collectionKeywords := dbSession.DB(DB_NAME).C(DB_COLLECTION_KEYWORD)
	collectionKeywords.Find(bson.M{"keyword":keyword.Name,"path":keyword.Path}).One(&keyword)
	
	var news []News
	collectionNews := dbSession.DB(DB_NAME).C(DB_COLLECTION_NEWS)
	collectionNews.Find(bson.M{"keywords":keyword.ID}).All(&news)
	
	collectionRecords := dbSession.DB(DB_NAME).C(DB_COLLECTION_RECORD)
	for i := range news {
		var n News
		collectionRecords.Find(bson.M{"_id":bson.ObjectIdHex(news[i].NewsID)}).One(&n)
		n.NewsID = news[i].NewsID
		n.KeywordIDs = news[i].KeywordIDs
		news[i] = n 
	}
	
	fmt.Println("input",len(news))
	var newsGroup []NewsGroup = generateNewsGroup(news)
	fmt.Println("output",len(newsGroup))
	var newsList string
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
		/*m := validPath.FindStringSubmatch(r.URL.Path)
		if m == nil {
			http.NotFound(w, r)
			return
		}*/

		//title := result.Title
		//fmt.Println("Phone:", result.mainStory)

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

