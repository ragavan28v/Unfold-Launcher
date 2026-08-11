import sys
path = r"D:\PROJECTS\AndroidStudioProjects\unfold\feature\feature-home\src\main\java\com\unfold\feature\home\HomeScreen.kt"
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()
stack = []
line = 1
i = 0
in_double = False
in_single = False
in_line_comment = False
in_block_comment = False
while i < len(text):
    ch = text[i]
    nxt = text[i+1] if i+1 < len(text) else ''
    if ch == '\n':
        line += 1
        in_line_comment = False
        i += 1
        continue
    if in_line_comment:
        i += 1
        continue
    if in_block_comment:
        if ch == '*' and nxt == '/':
            in_block_comment = False
            i += 2
            continue
        else:
            i += 1
            continue
    if not in_double and not in_single:
        if ch == '/' and nxt == '/':
            in_line_comment = True
            i += 2
            continue
        if ch == '/' and nxt == '*':
            in_block_comment = True
            i += 2
            continue
    if ch == '"' and not in_single and not in_line_comment and not in_block_comment:
        # toggle double quote unless escaped
        # count preceding backslashes
        bs = 0
        j = i-1
        while j >=0 and text[j] == '\\':
            bs += 1
            j -= 1
        if bs % 2 == 0:
            in_double = not in_double
        i += 1
        continue
    if ch == "'" and not in_double and not in_line_comment and not in_block_comment:
        bs = 0
        j = i-1
        while j >=0 and text[j] == '\\':
            bs += 1
            j -= 1
        if bs % 2 == 0:
            in_single = not in_single
        i += 1
        continue
    if in_double or in_single:
        i += 1
        continue
    if ch == '{':
        stack.append((line, i))
    elif ch == '}':
        if stack:
            stack.pop()
        else:
            print('Unmatched } at line', line)
    i += 1

print('Total unmatched opening braces left on stack:', len(stack))
if stack:
    for idx, (ln, pos) in enumerate(stack[-10:], start=1):
        print(f"#{idx}: line {ln}")
    # show context for last unmatched
    ln = stack[-1][0]
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    start = max(1, ln-10)
    end = min(len(lines), ln+10)
    print(f"\nContext around last unmatched at line {ln}:")
    for n in range(start, end+1):
        prefix = '>' if n==ln else ' '
        print(f"{prefix} {n:4d}: {lines[n-1].rstrip()}")
