package main

import (
    "testing"
    "os"
)

func init() {
	//change dir
	 os.Chdir("../..")
}

func TestGenerateVectorValue(t *testing.T) {
	teamplate :=  []string{"a","b","c","d","e"}
	keyword := []string{"a","c","e"}
	expect := []int{1,0,1,0,1}
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
	if calculateCosineSimilarity(vector1,vector2) != 1 {
		t.Error("calculateCosineSimilarity 1 was failed.")
	}
}

func TestCalculateCosineSimilarity2(t *testing.T) {
	vector1 := []int{1,0,1,0,1}
	vector2 := []int{0,1,0,1,0}
	if calculateCosineSimilarity(vector1,vector2) != 0 {
		t.Error("calculateCosineSimilarity 2 was failed.")
	}
}

func TestCalculateCosineSimilarity3(t *testing.T) {
	vector1 := []int{1,1,1,0,1}
	vector2 := []int{1,0,1,1,1}
	if calculateCosineSimilarity(vector1,vector2) != 3/4.0 {
		t.Error("calculateCosineSimilarity 3 was failed.")
	}
}

func TestReadTopicNameXML1(t *testing.T) {
	if readTopicNameXML("Football","").Name != "Football" {
		t.Error("readTopicNameXML 1 was failed.")
	}
}

func TestReadTopicNameXML2(t *testing.T) {
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




