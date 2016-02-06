**Gephi Server by Rob Nunn**
===
A render server for Gephi network graphs.

Using Gephi Toolkit 0.9+

REST requests.

SVG and PDF response

Usage
---

Synchronous

http://localhost:8080/gephiserver/rest/graph/stdSvg?graphId=1

Asynchronous

http://localhost:8080/gephiserver/rest/graph/stdSvgAsync?graphId=1 -> uuid -> http://localhost:8080/gephiserver/rest/graph/getSvgAsyncResult?uuid=[uuid]