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
//	"unicode/utf8"
)

const (
	SESSION_NAME_TOPIC_HANDLER = "TopicHandler"
	SESSION_KEY_PREVIOUS_TOPIC = "PreviousTopic"
	SESSION_KEY_TOPIC_PATH = "TopicPath"
	PATH_SEPARATER = "/";
	DB_NAME = "demo";
	DB_COLLECTION_RECORD = "News";
	DB_COLLECTION_NEWS = "modeling";
	DB_COLLECTION_KEYWORD = "keywords";
	COSINE_THRESHOLD = 0.8;
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
	//Keywords	[]Keyword		
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
	var vectorTemplate []string
	var contains bool
	for _,news := range allNews {	
		for _,keyword := range news.KeywordIDs {	
			contains = false
			for _,v := range vectorTemplate {	
				if v == keyword {
					contains = true
					break
				}
			}
			if contains {
				// already add keyword into vector
				continue
			}
			vectorTemplate = append(vectorTemplate,keyword)
		}
	}
	
	var allVectorValue [][]int = make([][]int,len(allNews))
	
	// find vector value
	for i := range allNews {	
		allVectorValue[i] = generateVectorValue(vectorTemplate,allNews[i].KeywordIDs)
	}
	
	// calculate consine similarity
	var calculateResult [][]float64 = make([][]float64,len(allNews))
	for i := range calculateResult {
		calculateResult[i] = make([]float64,len(allNews))
	}
	for i := 0; i<len(allVectorValue) ; i++ {	
		for j := i+1; j<len(allVectorValue) ; j++ {	
			calculateResult[i][j] = calculateCosineSimilarity(allVectorValue[i],allVectorValue[j])
		}
	}
	
	// group[i] = j <-> news j is in group i
	var group []int = make([]int,len(allNews))
	// set default value, news i is in group i
	for i := 0; i<len(group) ; i++ {	
		group[i] = i;
	}
	for i := 0; i<len(allVectorValue) ; i++ {	
		if group[i] != i {
			/* 
			 * if news i already move a group, it assume that pair of similar which news i 
			 * should be in the same group with news i
			*/
			continue
		}
		for j := i+1; j<len(allVectorValue) ; j++ {	
			if calculateResult[i][j] > COSINE_THRESHOLD {
				fmt.Println(i,j,calculateResult[i][j])
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
		var news []News
		for j := 0; j<len(group) ; j++ {
			if group[j] == number[i] {
				news = append(news,allNews[j])
			}
		}
		result[i] = NewsGroup{"","",news}	
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
	/*session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()

	// Optional. Switch the session to a monotonic behavior.
	session.SetMode(mgo.Monotonic, true)

	c := session.DB("gmsTry").C("gmsNews")
	*/

	
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
		id := strings.Split(news[i].NewsID,"\"")[1]
		fmt.Println(id)
		collectionRecords.Find(bson.M{"_id":bson.ObjectIdHex(id)}).One(&n)
		n.NewsID = news[i].NewsID
		n.KeywordIDs = news[i].KeywordIDs
		news[i] = n 
	}
	
	
	fmt.Println("input",len(news))
	var newsGroup []NewsGroup = generateNewsGroup(news)
	fmt.Println(len(newsGroup))
	for i:= range newsGroup{
		fmt.Println(i)
		for j:= range newsGroup[i].News {
			fmt.Println(newsGroup[i].News[j].Title)
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


var funcMap = template.FuncMap{
        // The name "inc" is what the function will be called in the template text.
        "inc": func(i int) int {
            return i + 1
        },
}

var templates = template.Must(template.New("test").Funcs(funcMap).ParseFiles("timeline.html"))

func renderTemplate(w http.ResponseWriter, tmpl string, p *Topic) {
	// Execute the template for each recipient.
	err := templates.ExecuteTemplate(w, tmpl+".html", p)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
	/*for _, r := range *p {
		err := templates.ExecuteTemplate(w, tmpl+".html", r)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
		}
	}*/

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

