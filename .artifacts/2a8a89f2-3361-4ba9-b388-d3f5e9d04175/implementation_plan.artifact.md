# Fix Recommended Routes Refresh Issue

The issue where recommended routes refuse to change is primarily caused by an unfiltered Firebase listener that overwrites the UI with a static list of routes whenever a search is performed. Additionally, multiple redundant listeners are created on every search, leading to race conditions and a "stuck" UI.

## User Review Required

> [!IMPORTANT]
> The Firebase listener will be changed to filter routes based on the current origin and destination, similar to how the local GTFS routes are filtered. If Firebase routes do not have stop information for spatial filtering, they may be excluded from the results to ensure relevance.

## Proposed Changes

### [MainActivity](file:///Users/francinnekayemanatad/Downloads/mobdeve-LAKBAYPH-jirvu-new-search-and-navbar/app/src/main/java/com/mobdeve/x21a/manatad/francinne/lakbay/MainActivity.kt)

#### [MODIFY] [MainActivity.kt](file:///Users/francinnekayemanatad/Downloads/mobdeve-LAKBAYPH-jirvu-new-search-and-navbar/app/src/main/java/com/mobdeve/x21a/manatad/francinne/lakbay/MainActivity.kt)
- Refactor `fetchRecommendedRoutes()` to:
    - Use `addListenerForSingleValueEvent` instead of `addValueEventListener` (or manage the listener correctly) to prevent redundant background tasks.
    - Clear the current `RecyclerView` adapter or show a loading state when a new search begins.
    - If Firebase routes are used, apply proximity filtering to ensure they match the user's origin and destination.
    - Ensure that if no new routes are found, the UI reflects this instead of falling back to stale data from the previous search.
- Move Geocoder calls in `updateDestination` to a background thread to prevent UI freezes.
- Standardize the `Route` object creation to ensure `routeId` and other metadata are handled consistently between GTFS and Firebase sources.

## Verification Plan

### Manual Verification
1.  **Test Origin Change**: Set a destination, then change the origin to various locations. Verify that the "Recommended Routes" list updates accordingly.
2.  **Test Destination Change**: Keep an origin, then change the destination. Verify that the routes list reflects the new path.
3.  **Empty Results**: Search for locations where no public transport routes are expected (e.g., very remote areas). Verify that the list clears or shows "No routes found" rather than displaying previous results.
4.  **UI Responsiveness**: Ensure the app doesn't hang or freeze when searching (Geocoding should be off-thread).
