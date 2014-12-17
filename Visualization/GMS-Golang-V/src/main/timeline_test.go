/* 
 * Unit testing of timeline.go
 * Testing 3 functionality
 * 1) generateVectorValue
 * 2) calculateCosineSimilarity
 * 3) readTopicNameXML and readXML
 */

package main

import (
    "testing"
    "os"
)

func init() {
	//change dir to default package (same place as timeline.go)
	 os.Chdir("../..")
}

func TestGenerateVectorValue(t *testing.T) {
	teamplate :=  []string{"a","b","c","d","e"}
	keyword := []string{"a","c","e"}
	expect := []int{1,0,1,0,1}  //expected result
	result := generateVectorValue(teamplate,keyword)
	for i:=0; i<len(result);i++ {
		if result[i] != expect[i] {
			t.Error("generateVectorValue was failed.")
			break
		}
	}
}

func TestCalculateCosineSimilarity1(t *testing.T) {
	vector1 := []int{1,0,1,0,1}
	vector2 := []int{1,0,1,0,1}
	//expected result is 1
	if calculateCosineSimilarity(vector1,vector2) != 1 {
		t.Error("calculateCosineSimilarity 1 was failed.")
	}
}

func TestCalculateCosineSimilarity2(t *testing.T) {
	vector1 := []int{1,0,1,0,1}
	vector2 := []int{0,1,0,1,0}
	//expected result is 0
	if calculateCosineSimilarity(vector1,vector2) != 0 {
		t.Error("calculateCosineSimilarity 2 was failed.")
	}
}

func TestCalculateCosineSimilarity3(t *testing.T) {
	vector1 := []int{1,1,1,0,1}
	vector2 := []int{1,0,1,1,1}
	//expected result is 0.75
	if calculateCosineSimilarity(vector1,vector2) != 3/4.0 {
		t.Error("calculateCosineSimilarity 3 was failed.")
	}
}

func TestReadTopicNameXML1(t *testing.T) {
	//Check the topic that have the same name as input
	if readTopicNameXML("Football","").Name != "Football" {
		t.Error("readTopicNameXML 1 was failed.")
	}
}

func TestReadTopicNameXML2(t *testing.T) {
	// check name, path, parent name of the topic's result
	path := "News"+PATH_SEPARATER+"Economics"+PATH_SEPARATER+"Business"
	topic := readTopicNameXML("Employment",path)
	if topic.Name != "Employment" {
		t.Error("readTopicNameXML 2.1 was failed.")
	}else if topic.Path != path {
		t.Error("readTopicNameXML 2.2 was failed.")
	}else if topic.ParentName != "Business" {
		t.Error("readTopicNameXML 2.3 was failed.")
	}
}

func TestReadXML(t *testing.T) {
	topic := readXML()
	if topic.Name != "News" {
		t.Error("readXML 1.1 was failed.")
	}else if len(topic.SubTopics) == 0 {
		t.Error("readXML 1.2 was failed.")
	}
}




