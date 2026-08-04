# Lakbay Mobile App (Major Course Output for MOBDEVE)

***
### Description
The application that the group has decided to build is named Lakbay. It is a Filipino commuting app that centers on making the Filipinos’ commuting experience more confident, enjoyable, and less daunting. In Lakbay, users can log-in, see available and updated routes, be involved in a community that allows Filipino commuters to suggest, and inform other Filipino commuters of an updated commuting route around Metro Manila. Once a user logs in the app, they are able to search a route by typing their starting point and destination point. Afterwards, the user’s search results would show various options to get from point A to point B. The search results would also show which routes would take the shortest amount of time to longest. Once a route was chosen, the user can follow the (where to board and be dropped off) route shown in Google Maps. Of course, the app has a community feature where Filipino commuters would be able to inform the developers and other commuters of the new and/or updated routes within Metro Manila. 

***

### Services/APIs
#### Web Server (Firebase)
- To store / verify user credentials and available programs
- To store community-submitted route updates, reports, and route information.
- To synchronize route data across all users in real time.
  
#### Geolocation (Google Maps)
- To determine the user's current location.
- To help users find nearby transportation routes, terminals, and stops.

#### Service (Background running)
- To continuously monitor the user’s GPS location while the app is in the background.
- To update the user when it enters its destination or update the user on its current commute status.

#### GTFS (General Transit Feed Specification) 
- To provide exact geographic locations of stops, route shapes, and schedule times of public transportation.
- To display live ETAs and locations of active public transport.


| Function | Description |
| --- | --- |
| Register| The user must first register an account before accessing the other features of the app. Users are required to give their full name, birthday, email, and password. |
| Log-in | The user must log-in before using the app. This requires the user to enter his/her registered email and a password. |
| Search Route | The user can enter their origin and destination to find commuting routes. |
| View Route Options | The application displays multiple route options, including estimated travel time and transfers needed. |
| View Route Details | The user can view complete route information including: <ul><li>Transportation Modes</li><li>Boarding Points</li><li>Drop-off Points</li><li>Boarding Estimates</li></ul>|
| Use Current Location | The user can use their current GPS location as their starting point when searching for routes. |
| Suggest Routes/Submit Route Updates | The users can suggest new routes or submit changes in existing routes. |
| Report Congestion | The user can update the remote database with the current congestion level of the terminal they are at. |
| Arrival Notification | The app will notify the user when the background location service detects the user is within a set radius of their destination. |
| View Terminal Status | Upon selecting a terminal on the map, the app will provide complete details, including real-time crowdsourced congestion data (e.g., "Empty," "Moderate," "Heavy").|

