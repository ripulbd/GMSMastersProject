package main

import (
	"fmt"
	"flag"
	"encoding/xml"
    "io"
    "os"
    "path/filepath"
)

//type Topic struct{
//	Keywords 	[]string
//	SubTopics 	[]Topic
//	ParentTopic *Topic
//}

type Topic struct{
	Name		xml.Name		`xml:"topic"`
	SubTopics 	[]SubTopic		`xml:"subtopic"`
}

type SubTopic struct{
	Name 		string 			`xml:"name,attr"` 
	SubTopics	[]SubTopic 		`xml:"subtopic"`
	Keywords	[]Keyword		`xml:"keyword"`
}

type Keyword struct{
	Name 		string 			`xml:"name,attr"` 
}

func ReadXML(reader io.Reader) ([]SubTopic, error) {
   
    decoder := xml.NewDecoder(reader)
    var topic Topic
	results := make([]SubTopic,10)
	var inElement string
	total := 0
    for { 
	  	t,_ := decoder.Token()
	   	if t == nil { 
        	break 
   		} 
	   	switch se := t.(type) { 
    	case xml.StartElement:
    		inElement = se.Name.Local
    		if inElement == "subtopic" {
    			var t1 SubTopic 
    			decoder.DecodeElement(&t1, &se)
    			fmt.Println(t1.Name)
    			fmt.Println(t1.SubTopics)
    			fmt.Println(t1.Keywords)
    			results[total] = t1
    			total++
    		}else if inElement =="topic"{
    			decoder.DecodeElement(&topic, &se)
    			fmt.Println(topic)
    		}
    	}
    }

    return topic.SubTopics	, nil
}

func main() {
	flag.Parse()

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


    xmlDemo, err := ReadXML(file)
    if err != nil {
        fmt.Println(err)
        os.Exit(1)
    }

    fmt.Printf("Key: %s ", xmlDemo[0].SubTopics[2].SubTopics[0].Keywords[1])
	
}

