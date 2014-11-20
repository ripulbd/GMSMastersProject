f = open("results", "rb")
content = f.read()
text = content.split("file:/Users/wyatt/Documents/Code/mallet-2.0.7/./data/")

nameEntity = []
number = []

for info in text:
	tmp = info.split("\n")
	index = tmp[0]
	words = tmp[1:]
	for word in words:
		sstr = word.split("->")
		for sin in sstr[:-1]:
			if not sin in nameEntity:
				nameEntity.append(sin)
				number.append("")
				number[nameEntity.index(sin)] += " "+sstr[-1]

for i in range(len(number)):
	print nameEntity[i], number[i]
