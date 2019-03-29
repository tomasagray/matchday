package self.me.matchday.fileserver;

class ICDManagerTest
{
    private static final String LOG_TAG = "ICDManagerTest";
/*

    // TODO:
    //  - Rewrite this test

    private static final String testURL = "http://galatamanhdf.blogspot.com/feeds/posts/default?alt=json";
    private static final List<URL> urls = new ArrayList<>();
    private static final ICDManager icmd = ICDManager.getInstance();
    private static final int URL_LIMIT = 10;
    private static FSUser user;

    @BeforeAll
    static void setup() throws IOException
    {
        // Setup user
        user = new ICDUser("burnergm112211@gmail.com" );
        user.setPassword("abithiw2itb");
        user.setKeepLoggedIn(true);

        // Setup blog
        Blogger blog = new Blogger(
                new URL(testURL),
                new GalatamanPostProcessor()
        );

        Log.i( LOG_TAG, "Successfully created: " + blog.toString());
        // Populate url list with all available URLs,
        // regardless of match/post
        blog
            .getEntries()
//            .map(p -> (GalatamanPost)p)
//            .map(GalatamanPost::new)
            .stream()
            .limit(URL_LIMIT)
            .forEach(gp -> {
                if(gp instanceof GalatamanPost)
                {
                    ((GalatamanPost) gp).getSources()
                            .stream()
                            .map(GalatamanMatchSource::getURLs)
                            .forEach(urls::addAll);
                }
            });

        Log.i( LOG_TAG, "Found: " + urls.size() + " URLs");
    }

    /**
     * Provider method for tests
     * @return A Stream of Arguments (of URLs)
     */
/*
    private static Stream<Arguments> getUrls()
    {
        return
                urls
                    .stream()
                    .map(Arguments::of);
    }

    @Test
    @DisplayName("Test the login function")
    void login()
    {
        ICDManager icd = ICDManager.getInstance();
        Log.i( LOG_TAG, "POSTing to URL: " + ICDManager.ICDData .getLoginURL());
        Log.i( LOG_TAG, "Data:\n" + user.toString());
        // Attempt to login
        icd.login(user);

        // Print results
        Log.i( LOG_TAG, "Response:\n" + icd.getLoginResponse());
        Log.i( LOG_TAG, "Login Successful?:\n" + icd.isLoggedIn() +"\n" );

        // Run the tests
        assertTrue( icd.isLoggedIn() );
        assertTrue( icd.getCookies().isPresent() );

        Log.i( LOG_TAG, "Cookies:\n---------------------------------------------------------------------");
        if(icd.getCookies().isPresent())
        {
            icd.getCookies().get().forEach(cookie ->
                Log.i( LOG_TAG,
                "Name: " + cookie.getName() +
                    "\n\tValue: " + cookie.getValue() +
                    "\n\tDomain: " + cookie.getDomain() +
                    "\n\tPath: " + cookie.getPath() +
                    "\n\tMaxAge: " + cookie.getMaxAge()
                )
            );
        }
    }

    @Disabled
    @Test
    void logout() {
    }

    @Disabled
    @Test
    void isLoggedIn() {
//        assertTrue();
    }

    @Disabled
    @Test
    void saveCookieData() {
    }

    @Disabled
    @Test
    void loadCookieData() {
    }

    @Test
    @DisplayName("Get page link with locally saved cookie text files")
    void testWithLocalCookies() throws IOException
    {
        // Get the URL of the first link
        URL pageURL = urls.get(0);
        URLConnection conn = pageURL.openConnection();
        conn.setRequestProperty("Cookie", getStoredCookiesString());
        // Connect!
        conn.connect();

        // Read the page
        String result
                = new BufferedReader(
                    new InputStreamReader(
                conn.getInputStream()
                ))
                .lines()
                .collect(
                        Collectors.joining("\n")
                );
        // Parse the result
        Document resultDoc = Jsoup.parse(result);
        Elements links = resultDoc.getElementsByClass("downloadnow");
        // Make sure we have at least one entry
        assertFalse(links.isEmpty());
        String theLink = links.first().attr("href");
        // Did we get the link?
        assertTrue(
                theLink.matches("https://d\\d{2}.inclouddrive.com/download.php\\?accesstoken=([A-z,0-9-])*")
        );
    }

    @Test
    @DisplayName("Get page link with dynamic cookies")
    void testWithDynamicCookies() throws IOException
    {
        // Create a manager
        ICDManager icdm = ICDManager.getInstance();
        // Login
        icdm.login(user);

        // Print cookies
        Log.i( LOG_TAG, "Cookies from login:\n----------------------------------------------------");
        if(icdm.getCookies().isPresent())
            icdm.getCookies().get().forEach((cookie) -> Log.i(LOG_TAG, cookie.toString()));

        URL testURL = urls.get(0);
        URLConnection conn = testURL.openConnection();
        conn.setRequestProperty( "Cookie", icdm.getCookieString() );
        Log.i( LOG_TAG, "Connection cookies:\n----------------------------------------------------");
        Log.i( LOG_TAG, conn.getRequestProperty("Cookie"));

        // Connect
        conn.connect();
        try(InputStream is = conn.getInputStream()) {
            StringBuilder sb = new StringBuilder();
            int i;
            // - Read the response, one byte at a time
            //   until the end of the stream
            while( (i = is.read()) != -1 )
                sb.append( (char)i );

            String result = sb.toString();
            Document doc = Jsoup.parse(result);
            Elements elems = doc.getElementsByClass("downloadnow");
            assertFalse(elems.isEmpty());
            String theLink = elems.first().attr("href");
            // Test link
            assertTrue(
                    theLink.matches("https://d\\d{2}.inclouddrive.com/download.php\\?accesstoken=([A-z,0-9-])*")
            );

            Log.i( LOG_TAG, "\n\nThe Link:\n--------------------------------");
            Log.i( LOG_TAG, theLink);

        }

    }

    @Tag("ICD")
    @DisplayName("Test the getDownloadURL method")
    @ParameterizedTest(name=" for: {index}: {0}")
    @MethodSource("getUrls")
    void getDownloadURL(URL url) throws IOException
    {
        String theLink = "";

        Log.i(LOG_TAG, "Testing url: " + url.toString());
        Optional<URL> op_url = icmd.getDownloadURL(url);
        if (op_url.isPresent()) {
            URL dlLink = op_url.get();
            theLink = dlLink.toString();

            Log.i(LOG_TAG, "\t|_Found link: " + theLink);
        }

        // Run the test
        assertTrue(
                theLink.matches("https://d\\d{2}.inclouddrive.com/download.php\\?accesstoken=([A-z,0-9-])*")
        );
    }

    @Disabled
    @Test
    @DisplayName("Logout, make sure we CAN'T read page")
    void testLogoutDisablesPageRead()
    {

    }

    // Test helper functions
    // -------------------------------------------------------------------
    @NotNull
    private String getStoredCookiesString() throws IOException {
        String root = "http://10.0.0.5/cookie_test/";
        Map<String, String> cookies = new HashMap<>();
        /**/
/*
        cookies.put(
                "__utma",
                TextFileReader.readRemote(new URL(root + "utma.txt"))
        );
        cookies.put(
                "__utmb",
                TextFileReader.readRemote(new URL(root + "utmb.txt"))
        );
        cookies.put(
                "__utmc",
                TextFileReader.readRemote(new URL(root + "utmc.txt"))
        );
        cookies.put(
                "__utmz",
                TextFileReader.readRemote(new URL(root + "utmz.txt"))
        );
        cookies.put(
                "userdata",
                TextFileReader.readRemote(new URL(root + "userdata.txt"))
        );
        cookies.put(
                "PHPSESSID",
                TextFileReader.readRemote(new URL(root + "PHPSESSID.txt"))
        );

        final StringBuilder sb = new StringBuilder();
        cookies.forEach((k, v) -> {
            sb
                    .append(k)          // key
                    .append("=")
                    .append(v)          // value
                    .append("; ");      // spacer
        });

        return sb.toString();
    }
    */
}