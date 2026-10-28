Task 1:
My understanding of the task here is to implement some sort of key value store that allows us to do conurrent reads safely and consistenly, but writes are allowed to be serialized which means they can be done synchronized as in one at a time.

Initial concerns and questions:
- Not allowed to copy whole map on a write. Initially i was thinking a history with the snapshot of the map object at that version.
- Need to come up with some logic to allow writes to block but reads to properly read, this means that i need to figure out some mechanism of guaranteed reads are good. (some sort of transaction logic, maybe atomic is needed)
- Reading mid operation?
- snapshot O(1) if i cant copy then how?

Thought dump:
- Writing is like a transaction almost, if partial write then we need to rollback to last known version.
- Things we need to track, map and version.
- Version should be the last thing that updates. This means that we need to update the map safely first.
  - This allows us to read the last version immutable before the new version has been committed if a read occurs same time as write.
  - Need some sort of versioning system. Thinking per key versioning since we cannot store entire map object. TODO!!!!
  - Immutability is key here.
  - On a completed operation we incremeent global version. Atomic or volatile?
    - Since writes are one at a time we can make version volatile. synchronized write methods.

Proposed design:

Some sort of key versioning which is a map of version,value. {1:hey,2:hello,3:test}..this gives us version and values. What about key? Some lookup, new map is needed.
Map of map. Map<K,Map<long,V>> something like this?

Looking at the Change i think we can have version,event since remove cannot be nulls. NULL NOT ALLOWED.

REVISED: private final Map<K,Map<long,Change>> something like that. (making it final so it cannot be swapped, we just want to change the contents, not reassign object)
private volatile long currentVersion

What maps, outer one obviously neds to be concurrentHashMap. Inner one needs to be ordered so we can check what the last event was ex. currentV = 4 but we ask for 99, we should get 4. Instead of some count iteration we can use ConcurrentSkipListMap (Claude says, need to double check what this structure actually is) logn over O(1) but saves me computing this logic manually. Worth.

Final design:

historyMap which is ConcurrentHashMap<K,ConcurrentSkipListMap<long,Change>>
version

version is volatile so it is all or nothing with synchronized logic, volatile will make it available to other threads right away, can only be changed by one thread at a time.
map now has history versioned without needing a copy.

We dont need a rollback, just dont commit unless successful, then last version is always good.

Decisions made so far:
- Shallow and deep clones are O(n) and snapshot needs O(1).
- -1 to symbolize removed but this doesnt work for generics as per contract, going with Change instead.
- currentVersion is global since the overall state needs to know its current version. History has granular versioning inside it. Not separate logic since its all relying on success or failed state for outer state. Considered moving history out but makes more sense to keep it all or nothing.
- null check before locking to not have wasted thread blocking.

Complexities:
put: concurrentHashmap is O(1) but adding into the ConcurrentSkipList is log(n) --> log(n)
getAt: hash lookup on key O(1) but floorEntry is traversing which is log(n) --> log(n)
get: calls getAt so same.

Skipped snapshot and changedSince due to time constraint. The design for this task took a long time for me.




Task 2:
- Started by reading classes like QueryContentHandler and FindUsersHandler to check how to implement the ScriptBean initialize method. Went into the BeanContextImpl as well just to validate my understanding.
- Took 30 min to get set up
- Took another 30 min to understand how to even begin to navigate the codebase. Was a little lost and needed AI help here to help me parse the large codebase for relevant sections

Answers to questions:
1. Honestly i skipped this part since the assignment has taken quite some time already, but i can say that its returning a supplier as an efficient way to only get the bean when needed. Almost like some form of lazy loading, but instead of loading lazily which is fetch and create once, supplier just provides an instance.
2. I think it would still use your authenticated user cookies since the context exists even though the bean isnt fetching it, its still set and the request still goes through the doFilter. But you wouldnt be able to run it with admin i think. I think if its restricted only to admin, then it should ideally throw a 403 forbidden if this is truly an admin only operation.
3. Skipped this due to time.

Approach here and what was done quickly and what was done in depth:
- Used AI to quickly onboard vs manual onboarding in terms of CLI install and setting up the project
- Used AI to parse large codebase to figure out what my starting point is, this allowed me to read relevant classes and figure out the syntax i need and classes I can use to query
- Tested some objects manually first slowly to check what they were doing and how i can use them, used AI to clean this up to accelerate a lot here.
- Once i understood that there is a querying mechanism i sort of built my approach around querying by group and then counting each group and serializing it in a response.

A few things i decided to skip was deep diving into the codebase to understand each bit and piece, since the point was to create a lightweight rest endpoint i decided to use AI to fetch the relevant pieces only, and diving as deep as like ensuring the recursive nature of traversing the nodes did what i thought the function did.

Sample curl:

curl 'http://localhost:8080/api/com.example.inventory:inventory?path=/mysite' \
  -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8' \
  -H 'Accept-Language: en-US,en;q=0.5' \
  -H 'Cache-Control: max-age=0' \
  -H 'Connection: keep-alive' \
  -b 'app.browse.RecentItemsList=portal%3Asite%7Cbase%3Afolder; JSESSIONID=ecdtevuaz71i4jxbt4ttf7b40' \
  -H 'Sec-Fetch-Dest: document' \
  -H 'Sec-Fetch-Mode: navigate' \
  -H 'Sec-Fetch-Site: none' \
  -H 'Sec-Fetch-User: ?1' \
  -H 'Sec-GPC: 1' \
  -H 'Upgrade-Insecure-Requests: 1' \
  -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36' \
  -H 'sec-ch-ua: "Not;A=Brand";v="8", "Chromium";v="150", "Brave";v="150"' \
  -H 'sec-ch-ua-mobile: ?0' \
  -H 'sec-ch-ua-platform: "macOS"'

  response:
  {
  "project": "default",
  "branch": "draft",
  "path": "/mysite",
  "total": 7,
  "byType": {
    "portal:template-folder": 2,
    "portal:site": 1,
    "base:folder": 4
  }
}
