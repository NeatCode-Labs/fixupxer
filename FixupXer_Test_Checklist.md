# FixupXer Test Checklist

## Test Instructions
For each scenario:
- Test both Share screen (via share intent) and Main screen (manual input)
- For each URL type, test both clean and dirty versions
- For toggle scenarios, test both ON and OFF states
- Note the exact output in the "Processed URL" field
- Report any discrepancies from expected behavior

---

## SHARE SCREEN TESTS

### No Toggle Shown Scenarios

#### 1. Clean Link (Other than supported domains)
**Test URL:**
```
https://www.google.com/search?q=test
```
**Expected:** Display "Nothing to do!" in Processed URL field
**Output:** Nothing to do!

#### 2. Dirty Link (Other than supported domains)
**Test URL:**
```
https://www.google.com/search?q=test&utm_source=test
```
**Expected:** Clean the link and show clean version
**Output:** https://www.google.com/search?q=test

---

### Toggle Shown Scenarios

#### Instagram.com Tests

#### 3. Clean instagram.com - Toggle OFF
**Test URL:**
```
https://www.instagram.com/p/ABC123/
```
**Expected:** Display "Nothing to do!" in Processed URL field
**Output:** https://www.instagram.com/p/ABC123/

#### 4. Clean instagram.com - Toggle ON
**Test URL:**
```
https://www.instagram.com/p/ABC123/
```
**Expected:** Convert to kkinstagram.com
**Output:** https://www.kkinstagram.com/p/ABC123/

#### 5. Dirty instagram.com - Toggle OFF
**Test URL:**
```
https://www.instagram.com/p/ABC123/?utm_source=test&igshid=xyz
```
**Expected:** Just clean the link
**Output:** https://www.instagram.com/p/ABC123/

#### 6. Dirty instagram.com - Toggle ON
**Test URL:**
```
https://www.instagram.com/p/ABC123/?utm_source=test&igshid=xyz
```
**Expected:** Clean the link and convert to kkinstagram
**Output:** https://www.kkinstagram.com/p/ABC123/

---

#### kkinstagram.com Tests

#### 7. Clean kkinstagram.com - Toggle OFF
**Test URL:**
```
https://www.kkinstagram.com/p/ABC123/
```
**Expected:** Convert to instagram.com
**Output:** https://www.instagram.com/p/ABC123/

#### 8. Clean kkinstagram.com - Toggle ON
**Test URL:**
```
https://www.kkinstagram.com/p/ABC123/
```
**Expected:** Display "Nothing to do!" in Processed URL field
**Output:** https://www.kkinstagram.com/p/ABC123/

#### 9. Dirty kkinstagram.com - Toggle OFF
**Test URL:**
```
https://www.kkinstagram.com/p/ABC123/?utm_source=test&igshid=xyz
```
**Expected:** Clean the link and convert to instagram.com
**Output:** https://www.instagram.com/p/ABC123/

#### 10. Dirty kkinstagram.com - Toggle ON
**Test URL:**
```
https://www.kkinstagram.com/p/ABC123/?utm_source=test&igshid=xyz
```
**Expected:** Just clean the link
**Output:** https://www.kkinstagram.com/p/ABC123/

---

#### x.com Tests

#### 11. Clean x.com - Toggle OFF
**Test URL:**
```
https://x.com/username/status/123456789
```
**Expected:** Display "Nothing to do!" in Processed URL field
**Output:** https://x.com/username/status/123456789

#### 12. Clean x.com - Toggle ON
**Test URL:**
```
https://x.com/username/status/123456789
```
**Expected:** Just convert to fixupx
**Output:** https://fixupx.com/username/status/123456789

#### 13. Dirty x.com - Toggle OFF
**Test URL:**
```
https://x.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Just clean the link
**Output:** https://x.com/username/status/123456789

#### 14. Dirty x.com - Toggle ON
**Test URL:**
```
https://x.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Clean the link and convert to fixupx
**Output:** https://fixupx.com/username/status/123456789

---

#### fixupx.com Tests

#### 15. Clean fixupx.com - Toggle OFF
**Test URL:**
```
https://fixupx.com/username/status/123456789
```
**Expected:** Convert to x.com
**Output:** https://x.com/username/status/123456789

#### 16. Clean fixupx.com - Toggle ON
**Test URL:**
```
https://fixupx.com/username/status/123456789
```
**Expected:** Display "Nothing to do!" in Processed URL field
**Output:** https://fixupx.com/username/status/123456789

#### 17. Dirty fixupx.com - Toggle OFF
**Test URL:**
```
https://fixupx.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Clean the link and convert to x.com
**Output:** https://x.com/username/status/123456789

#### 18. Dirty fixupx.com - Toggle ON
**Test URL:**
```
https://fixupx.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Just clean the link
**Output:** https://fixupx.com/username/status/123456789

---

#### fxtwitter.com Tests

#### 19. Clean fxtwitter.com - Toggle OFF
**Test URL:**
```
https://fxtwitter.com/username/status/123456789
```
**Expected:** Convert to x.com
**Output:** https://x.com/username/status/123456789

#### 20. Clean fxtwitter.com - Toggle ON
**Test URL:**
```
https://fxtwitter.com/username/status/123456789
```
**Expected:** Convert to fixupx.com
**Output:** https://fixupx.com/username/status/123456789

#### 21. Dirty fxtwitter.com - Toggle OFF
**Test URL:**
```
https://fxtwitter.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Clean the link and convert to x.com
**Output:** https://x.com/username/status/123456789

#### 22. Dirty fxtwitter.com - Toggle ON
**Test URL:**
```
https://fxtwitter.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Clean the link and convert to fixupx.com
**Output:** https://fixupx.com/username/status/123456789

---

#### twitter.com Tests

#### 23. Clean twitter.com - Toggle OFF
**Test URL:**
```
https://twitter.com/username/status/123456789
```
**Expected:** Convert to x.com
**Output:** https://x.com/username/status/123456789

#### 24. Clean twitter.com - Toggle ON
**Test URL:**
```
https://twitter.com/username/status/123456789
```
**Expected:** Just convert to fixupx
**Output:** https://fixupx.com/username/status/123456789

#### 25. Dirty twitter.com - Toggle OFF
**Test URL:**
```
https://twitter.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Clean the link and convert to x.com
**Output:** https://x.com/username/status/123456789

#### 26. Dirty twitter.com - Toggle ON
**Test URL:**
```
https://twitter.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Clean the link and convert to fixupx
**Output:** https://fixupx.com/username/status/123456789

---

## MAIN SCREEN TESTS

### No Toggle Shown Scenarios

#### 27. Clean Link (Other than supported domains)
**Test URL:**
```
https://www.google.com/search?q=test
```
**Expected:** Display "Nothing to do!" in Processed URL field
**Output:** Nothing to do!

#### 28. Dirty Link (Other than supported domains)
**Test URL:**
```
https://www.google.com/search?q=test&utm_source=test
```
**Expected:** Clean the link
**Output:** https://www.google.com/search?q=test

---

### Toggle Shown Scenarios

#### Instagram.com Tests

#### 29. Clean instagram.com - Toggle OFF
**Test URL:**
```
https://www.instagram.com/p/ABC123/
```
**Expected:** Display "Nothing to do!" in Processed URL field
**Output:** https://www.instagram.com/p/ABC123/

#### 30. Clean instagram.com - Toggle ON
**Test URL:**
```
https://www.instagram.com/p/ABC123/
```
**Expected:** Convert link to kkinstagram
**Output:** https://www.kkinstagram.com/p/ABC123/

#### 31. Dirty instagram.com - Toggle OFF
**Test URL:**
```
https://www.instagram.com/p/ABC123/?utm_source=test&igshid=xyz
```
**Expected:** Just clean the link
**Output:** https://www.instagram.com/p/ABC123/

#### 32. Dirty instagram.com - Toggle ON
**Test URL:**
```
https://www.instagram.com/p/ABC123/?utm_source=test&igshid=xyz
```
**Expected:** Clean the link and convert to kkinstagram
**Output:** https://www.kkinstagram.com/p/ABC123/

---

#### kkinstagram.com Tests

#### 33. Clean kkinstagram.com - Toggle OFF
**Test URL:**
```
https://www.kkinstagram.com/p/ABC123/
```
**Expected:** Convert to instagram.com
**Output:** https://www.instagram.com/p/ABC123/

#### 34. Clean kkinstagram.com - Toggle ON
**Test URL:**
```
https://www.kkinstagram.com/p/ABC123/
```
**Expected:** Display "Nothing to do!" in Processed URL field
**Output:** https://www.kkinstagram.com/p/ABC123/

#### 35. Dirty kkinstagram.com - Toggle OFF
**Test URL:**
```
https://www.kkinstagram.com/p/ABC123/?utm_source=test&igshid=xyz
```
**Expected:** Clean the link and convert to instagram.com
**Output:** https://www.instagram.com/p/ABC123/

#### 36. Dirty kkinstagram.com - Toggle ON
**Test URL:**
```
https://www.kkinstagram.com/p/ABC123/?utm_source=test&igshid=xyz
```
**Expected:** Just clean the link
**Output:** https://www.kkinstagram.com/p/ABC123/

---

#### x.com Tests

#### 37. Clean x.com - Toggle OFF
**Test URL:**
```
https://x.com/username/status/123456789
```
**Expected:** Display "Nothing to do!" in Processed URL field
**Output:** https://x.com/username/status/123456789

#### 38. Clean x.com - Toggle ON
**Test URL:**
```
https://x.com/username/status/123456789
```
**Expected:** Convert link to fixupx
**Output:** https://fixupx.com/username/status/123456789

#### 39. Dirty x.com - Toggle OFF
**Test URL:**
```
https://x.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Just clean the link
**Output:** https://x.com/username/status/123456789

#### 40. Dirty x.com - Toggle ON
**Test URL:**
```
https://x.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Convert link to fixupx and clean it
**Output:** https://fixupx.com/username/status/123456789

---

#### fixupx.com Tests

#### 41. Clean fixupx.com - Toggle OFF
**Test URL:**
```
https://fixupx.com/username/status/123456789
```
**Expected:** Convert to x.com
**Output:** https://x.com/username/status/123456789

#### 42. Clean fixupx.com - Toggle ON
**Test URL:**
```
https://fixupx.com/username/status/123456789
```
**Expected:** Display "Nothing to do!" in Processed URL field
**Output:** https://fixupx.com/username/status/123456789

#### 43. Dirty fixupx.com - Toggle OFF
**Test URL:**
```
https://fixupx.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Clean the link and convert to x.com
**Output:** https://x.com/username/status/123456789

#### 44. Dirty fixupx.com - Toggle ON
**Test URL:**
```
https://fixupx.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Just clean the link
**Output:** https://fixupx.com/username/status/123456789

---

#### fxtwitter.com Tests

#### 45. Clean fxtwitter.com - Toggle OFF
**Test URL:**
```
https://fxtwitter.com/username/status/123456789
```
**Expected:** Convert to x.com
**Output:** https://x.com/username/status/123456789

#### 46. Clean fxtwitter.com - Toggle ON
**Test URL:**
```
https://fxtwitter.com/username/status/123456789
```
**Expected:** Convert to fixupx.com
**Output:** https://fixupx.com/username/status/123456789

#### 47. Dirty fxtwitter.com - Toggle OFF
**Test URL:**
```
https://fxtwitter.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Clean the link and convert to x.com
**Output:** https://x.com/username/status/123456789

#### 48. Dirty fxtwitter.com - Toggle ON
**Test URL:**
```
https://fxtwitter.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** clean the link and convert to fixupx
**Output:** https://fixupx.com/username/status/123456789

---

#### twitter.com Tests

#### 49. Clean twitter.com - Toggle OFF
**Test URL:**
```
https://twitter.com/username/status/123456789
```
**Expected:** Convert to x.com
**Output:** https://x.com/username/status/123456789

#### 50. Clean twitter.com - Toggle ON
**Test URL:**
```
https://twitter.com/username/status/123456789
```
**Expected:** Just convert to fixupx
**Output:** https://fixupx.com/username/status/123456789

#### 51. Dirty twitter.com - Toggle OFF
**Test URL:**
```
https://twitter.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Clean the link and convert to x.com
**Output:** https://x.com/username/status/123456789

#### 52. Dirty twitter.com - Toggle ON
**Test URL:**
```
https://twitter.com/username/status/123456789?utm_source=test&ref_src=xyz
```
**Expected:** Clean the link and convert to fixupx
**Output:** https://fixupx.com/username/status/123456789

---

## UI ELEMENTS TO VERIFY

### Toggle Text
- [ ] Toggle shows "Create embeddable link?" (not "Use fixupx.com?" or "Use kkinstagram.com?")
- [ ] Toggle icon is a conversion icon (not the weird symbol)

### Toggle Visibility
- [ ] Toggle appears for: instagram.com, kkinstagram.com, x.com, fixupx.com, fxtwitter.com, twitter.com
- [ ] Toggle does NOT appear for: google.com, youtube.com, or other domains

---

## Test Summary

**Issues Found:**
- 

**Toggle Functionality Working Correctly:**
- 

**URL Processing Working Correctly:**
- 

**UI Elements Working Correctly:**
- 

**Additional Notes:**
- 