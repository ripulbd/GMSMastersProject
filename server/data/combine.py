# first keyid
# second path

flist = open("list3.json")
fwords = open("keyword.json")

clist = flist.readlines()
cwords = fwords.readlines()

for llist in clist: # "keywords":"[quot, added, year, told, eveningtimes, people, work, mr, place]","path":"News/Art;"}
	iid = llist.split("\"")		# iid[3]: 5412d53ce4b0eb14137b44b8
	words = iid[7][1:-1].split(",")		# keywords: [quot, added, year, told, eveningtimes, people, work, mr, place]
	print "{\"newsId\":\"ObjectId(\\\""+str(iid[3])+"\\\")\",",
	wordlist = ""
	for word in words:
		word = word.replace(" ", "")
		path = llist.split("\"")[11].split(";")		# News/Sport/Football/Club/Rangers;News/Location/Glasgow/G51/Ibrox;
		# in keyword.json
		for e in path:
			if len(e) < 2:
				continue
			for keyline in cwords:		#keyline {"id":"1","keyword":"albion","path":"News/Sport/Football/Club/Rangers"}
				# print word, keyline.split("\"")[7]
				if word == keyline.split("\"")[7] and e in keyline:
					# print keyline, word, e
					wordlist += "\""+keyline.split("\"")[3]+"\","
	print "\"keywords\":[" + wordlist[:-1] + "]",
	print "}"




'''		for line in cwords:
			judge = line.split(" ")
			judge[1] = judge[1].replace("\n","")
			if word == judge[1]:
#				print word,
				wordlist += "\""+line.split(" ")[0]+"\","


fwords = open("keywords")
flist = open("list3.json")

cwords = fwords.readlines()
clist = flist.readlines()

hashtable = {}

index = 1
for line in cwords:
	info = line.split(" ")
	info[1] = info[1].replace("\n","")		# info[1]: keyword
	for line2 in clist:
		keywordlist = line2.split("\"")[7]
		keyword = keywordlist[1:-1].replace(" ","").split(",")
		for ekey in keyword:
			if info[1] == ekey:
				for path in line2.split("\"")[11].split(";"): 	# [11]: path
					if len(path) < 1:
						continue
					tmp = info[1] + path
					if hashtable.has_key(tmp):
						continue
					else:
						print "{\"id\":\"" + str(index) + "\"," +\
							"\"keyword\":\"" + info[1] + "\"," +\
							"\"path\":\"" + path + "\"}"
						hashtable[tmp] = 1
						index+=1'''