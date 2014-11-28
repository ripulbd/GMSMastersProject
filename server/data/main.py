f = open("keyword.json")
content = f.readlines()

path = []

for line in content:
	info = line.split("\"")
	if len(info[11].split("/")) <=2:
		if not info[11] in path:
			path.append(info[11])
			print info[11]
		
