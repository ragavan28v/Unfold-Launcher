path = r"D:\PROJECTS\AndroidStudioProjects\unfold\feature\feature-home\src\main\java\com\unfold\feature\home\HomeScreen.kt"
with open(path,'r',encoding='utf-8') as f:
    lines = f.readlines()
count = 0
counts = [0]
for i,line in enumerate(lines, start=1):
    for ch in line:
        if ch == '{': count += 1
        elif ch == '}': count -= 1
    counts.append(count)
# line numbers are 1-based; counts[i] is cumulative after line i
line_before = 92
count_before = counts[line_before]
print('count before HomeScreen open (line 92):', count_before)
for i in range(93, len(counts)):
    if counts[i] == count_before:
        print('HomeScreen likely closed at line', i)
        break
else:
    print('No closure found; final balance:', counts[-1])
