package main

import (
	"fmt"
    //"gopkg.in/mgo.v2"
    //"gopkg.in/mgo.v2/bson"
	"flag"
	"encoding/xml"
	"encoding/json"
	"io"
	"os"
	"path/filepath"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"regexp"
	"html/template"
//	"unicode/utf8"
)

var (
	addr = flag.Bool("addr", false, "find open address and print to final-port.txt")
)

type Topic struct{
	Name 		string 			`xml:"name,attr"` 
	SubTopics	[]Topic 		`xml:"topic"`
	Keywords	[]Keyword		`xml:"keyword"`
}

type Keyword struct{
	Name 		string 			`xml:"name,attr"`
	Weight		int 
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
	xmlPath, err := filepath.Abs("xmlDemo.xml")
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

func readTopicNameXML(name string)(Topic){
	  topic := readXML()
	  topic = topicTraveller(topic,name)
	  return topic
}

// Depth first search
func topicTraveller(topic Topic,name string)(Topic){
	var  result Topic
	if topic.Name != name {
    	for _,t := range topic.SubTopics {
    		result = topicTraveller(t,name)
    		if result.Name == name {
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

func tagCloudHandler(w http.ResponseWriter, r *http.Request) {
	/*session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()

	// Optional. Switch the session to a monotonic behavior.
	session.SetMode(mgo.Monotonic, true)

	c := session.DB("gmsTry").C("gmsNews")
	*/
	var topic Topic
	topic.Keywords = []Keyword{{"ManU",10},{"Liverpool",5},{"Arsenal",15},{"Chelsea",8},{"Mango",2},{"Teams",7},{"Newcastle",5},{"Ruby",11},{"Dreams",10},{"FFF",17}}
	fmt.Println(topic)

	renderTemplate(w, "tagcloudTest", &topic)
}

func showListHandler(w http.ResponseWriter, r *http.Request) {
	/*session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()

	// Optional. Switch the session to a monotonic behavior.
	session.SetMode(mgo.Monotonic, true)

	c := session.DB("gmsTry").C("gmsNews")
	*/
	keyword := r.URL.Query()["keyword"][0]
	fmt.Println(keyword)
	
	topic := readTopicNameXML("Business")
	
	js, err := json.Marshal(topic)
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

var templates = template.Must(template.New("test").Funcs(funcMap).ParseFiles("timeline.html","tagcloudTest.html"))

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

func createSubtopicTags(w http.ResponseWriter, r *http.Request){

    //take the tagname from the url	and print it
	tagname := r.URL.Query()["tagname"][0];
	fmt.Printf("Query: %s\n", tagname)
	
	/*session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()*/
	
	//find the specific topic in XML which has equal name with tagname
	topic := readTopicNameXML(tagname);
	
	//send it back to html
	js, err := json.Marshal(topic)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
		
	w.Header().Set("Content-Type", "application/json")
  	w.Write(js)
}


func main() {
	flag.Parse()
	http.HandleFunc("/", makeHandler(timelineHandler))
	http.HandleFunc("/timeline/", makeHandler(timelineHandler))
	http.HandleFunc("/tagcloud/", makeHandler(tagCloudHandler))
	http.HandleFunc("/showlist", makeHandler(showListHandler))
	http.HandleFunc("/subtags", makeHandler(createSubtopicTags))
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

	http.ListenAndServe(":8090", nil)
	
}

