# DV01 backend challenge

I'm at about the time level asked and I hit a convenient stopping point so this is probably all there will ever be to this little app. The requirements were obviously extremely vague so I took some liberties with them. Chief among them was my decision to avoid using a database. I considered using an H2 database but decided it was pointless to use an in-memory database and my time was best spent elsewhere. 

What I ended up with was a Play application that will read in the big csv file once on the first load of any page and then keep the relevant fields of the data sitting in memory. Once loaded, a user can query the data set via a few mechanisms(all GETs for now). Examples for all endpoints are below:

* http://localhost:9000/ - Returns the "relevant" fields for all loaded data. The fields are compile-time constants and currently there are only a few
* http://localhost:9000/stats-by-field/loan_amnt - Takes the name of a numerical field and outputs (very) basic stats for it across the whole data set
* http://localhost:9000/agg-by-field/addr_state/loan_amnt - Takes a categorical field name and a numerical field name and returns the average for the latter grouped by the former. In this example it's outputting the average loan amount for each state.

Git isn't letting me upload the full csv file because it's too big, so this is using a truncated version of that file. I also had a really nice commit history prepared but had to blow it away because I didn't want to spend all three hours trying to remove one file from all my commits. To run, just fire off an `sbt run` in the main directory. Feel free to use the larger file, the initial load takes a second but the app is instantaneous afterwards.