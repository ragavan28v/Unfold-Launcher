path = r"D:\PROJECTS\AndroidStudioProjects\unfold\feature\feature-home\src\main\java\com\unfold\feature\home\HomeScreen.kt"
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
count = 0
counts = []
for i, line in enumerate(lines, start=1):
    for ch in line:
        if ch == '{':
            count += 1
        elif ch == '}':
            count -= 1
    counts.append((i, count, line.rstrip('\n')))

print('total lines:', len(lines))
print('final balance:', count)
print('\nLast 60 lines with cumulative brace count:')
for i, c, l in counts[-60:]:
    print(f"{i:4d}: {c:3d}  {l}")
 
max_count = max(counts, key=lambda x: x[1])
print('\nMax cumulative count:', max_count[1], 'at line', max_count[0])

# Also show first location where count increased without subsequent close
for i, c, l in counts:
    if c > 0 and (i == len(lines) or counts[i][1] <= c):
        pass
