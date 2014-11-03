f = open("mall")
content = f.read()
lines = content.split("###")

for line in lines:
	info = line.strip().split("|||")
	ff = open("./data/"+info[0], "w")
	ff.write(info[1])
	ff.close()
