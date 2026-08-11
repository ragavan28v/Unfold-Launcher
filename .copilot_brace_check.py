path = r"D:\PROJECTS\AndroidStudioProjects\unfold\feature\feature-home\src\main\java\com\unfold\feature\home\HomeScreen.kt"
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
count = 0
first_negative = None
first_zero_after_start = None
for i, line in enumerate(lines, start=1):
    for ch in line:
        if ch == '{':
            count += 1
        elif ch == '}':
            count -= 1
    if count < 0 and first_negative is None:
        first_negative = i
    if count == 0 and first_zero_after_start is None:
        first_zero_after_start = i
print('lines:', len(lines))
print('first_negative_line:', first_negative)
print('first_zero_after_start_line:', first_zero_after_start)
print('final_balance:', count)
