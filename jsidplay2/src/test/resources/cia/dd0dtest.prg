<!DOCTYPE html>
<!-- Server: sfn-web-6 -->


    


















<!--[if lt IE 7 ]> <html lang="en" class="no-js ie6"> <![endif]-->
<!--[if IE 7 ]>    <html lang="en" class="no-js ie7"> <![endif]-->
<!--[if IE 8 ]>    <html lang="en" class="no-js ie8"> <![endif]-->
<!--[if IE 9 ]>    <html lang="en" class="no-js ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]>-->
<html lang="en" class="no-js"> <!--<![endif]-->
<head>
    <meta content="text/html; charset=UTF-8" http-equiv="content-type"/>
    <title>
  VICE / Code /
  [r31403]
  /testprogs/CIA/dd0dtest/dd0dtest.prg
</title>
    


<script type="text/javascript">
  var _paq = _paq || [];
  _paq.push(['trackPageView']);
  _paq.push(['enableLinkTracking']);
  (function() {
    var u="//analytics.slashdotmedia.com/";
    _paq.push(['setTrackerUrl', u+'sf.php']);
    _paq.push(['setSiteId', 39]);
    var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
    g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'sf.js'; s.parentNode.insertBefore(g,s);
  })();
</script>
<noscript><p><img src="//analytics.slashdotmedia.com/sf.php?idsite=39" style="border:0;" alt="" /></p></noscript>

<meta id="project_name" name="project_name" content='vice-emu' />
<!--[if lt IE 7 ]>
  <script src="https://a.fsdn.com/allura/nf/1468509372/_ew_/theme/sftheme/js/sftheme/vendor/dd_belatedpng.js"></script>
  <script> DD_belatedPNG.fix('img, .png_bg'); //fix any <img> or .png_bg background-images </script>
<![endif]-->
<link href='//fonts.googleapis.com/css?family=Ubuntu:regular' rel='stylesheet' type='text/css'>
    
        <!-- ew:head_css -->

    
        <link rel="stylesheet"
                type="text/css"
                href="https://a.fsdn.com/allura/nf/1468509372/_ew_/_slim/css?href=allura%2Fcss%2Fforge%2Fhilite.css%3Ballura%2Fcss%2Fforge%2Ftooltipster.css"
                >
    
        <link rel="stylesheet"
                type="text/css"
                href="https://a.fsdn.com/allura/nf/1468509372/_ew_/allura/css/font-awesome.min.css"
                >
    
        <link rel="stylesheet"
                type="text/css"
                href="https://a.fsdn.com/allura/nf/1468509372/_ew_/theme/sftheme/css/forge.css"
                >
    
        
<!-- /ew:head_css -->

    
    
        <!-- ew:head_js -->

    
        <script type="text/javascript" src="https://a.fsdn.com/allura/nf/1468509372/_ew_/_slim/js?href=allura%2Fjs%2Fjquery-base.js%3Btheme%2Fsftheme%2Fjs%2Fsftheme%2Fvendor%2Fmodernizr.custom.90514.js%3Btheme%2Fsftheme%2Fjs%2Fsftheme%2Fshared_head.js"></script>
    
        
<!-- /ew:head_js -->

    

    
        <style type="text/css">
            #page-body.project---init-- #top_nav { display: none; }

#page-body.project---init-- #nav_menu_holder { display: none; margin-bottom: 0; }

#page-body.project---init-- #content_base {margin-top: 0; }
        </style>
    
    
    <link rel="alternate" type="application/rss+xml" title="RSS" href="/p/vice-emu/code/feed.rss"/>
    <link rel="alternate" type="application/atom+xml" title="Atom" href="/p/vice-emu/code/feed.atom"/>

    <style>.XNWQOIyTHmFkCRkhiLXzmto {
        display: none
    }</style>

    
    
    
    


<script type="text/javascript">
    (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
            (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
        m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
    })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

    function _add_tracking(prefix, tracking_id) {
        ga('create', tracking_id, {cookieDomain: 'auto', 'name': prefix});
        
        ga(prefix+'.set', 'dimension9', 'vice-emu');
        ga(prefix+'.set', 'dimension10', 'svn');
        
        ga(prefix+'.set', 'dimension13', 'Logged Out');
        ga(prefix+'.send', 'pageview');
    }
      _add_tracking('sfnt1', 'UA-32013-6');
      _add_tracking('sfnt2', 'UA-36130941-1');
    
</script>
</head>

<body id="forge">

    
        <!-- ew:body_top_js -->

    
        
<!-- /ew:body_top_js -->

    


<header id="site-header">
    <div class="wrapper">
        <a href="/" class="logo">
            <span>SourceForge</span>
        </a>
        
        <form method="get" action="/directory/">
            <input type="text" id="words" name="q" placeholder="Search">
        </form>
        
        <!--Switch to {language}-->
        <nav id="nav-site">
            <a href="/directory/" title="Browse our software.">Browse</a>
            <a href="/directory/enterprise" title="Browse our Enterprise software.">Enterprise</a>
            <a href="/blog/" title="Read the latest news from the SF HQ.">Blog</a>
            <a href="//deals.sourceforge.net/?utm_source=sourceforge&amp;utm_medium=navbar&amp;utm_campaign=homepage" title="Discover and Save on the Best Gear, Gadgets, and Software" class="featured-link" target="_blank">Deals</a>
            <a href="/support" title="Contact us for help and feedback.">Help</a>
            <a href="/create"  class="featured-link blue"  style="" title="Create and publish Open Source software for free.">Create</a>
        </nav>
        <nav id="nav-account">
            
              <div class="logged_out">
                <a href="/auth/">Log In</a>
                <span>or</span>
                <a href="https://sourceforge.net/user/registration/">Join</a>
              </div>
            
        </nav>
        
    </div>
</header>
<header id="site-sec-header">
    <div class="wrapper">
        <nav id="nav-hubs">
            <h4>Solution Centers</h4>
            <a href="http://goparallel.sourceforge.net/">Go Parallel</a>
        </nav>
        <nav id="nav-collateral">
            <a href="https://library.slashdotmedia.com/">Resources</a>
            <a href="/user/newsletters?source=sfnet_header">Newsletters</a>
            <a href="/cloud-storage-providers/?source=sfnet_header">Cloud Storage Providers</a>
            <a href="/business-voip/?source=sfnet_header">Business VoIP Providers</a>
            
            <a href="/call-center-providers/?source=sfnet_header">Call Center Providers</a>
        </nav>
    </div>
</header>

    
    
    

<section id="page-body" class=" neighborhood-Projects project-vice-emu mountpoint-code">
<div id="nav_menu_holder">
    
        
            



    
    
    
    
    <nav id="breadcrumbs">
        <ul>
            <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb"><a itemprop="url" href="/">Home</a></li>
            <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb"><a itemprop="url" href="/directory">Browse</a></li>
            
            
                
                
            
            
                
            
            
            
                <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb"><a itemprop="url" href="/p/vice-emu/">VICE</a></li>
                
            
            
                <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb">Code</li>
                
            
        </ul>
    </nav>
    
    
    
  
    
      <img src="/p/vice-emu/icon?2013-06-05 13:39:47+00:00" class="project_icon" alt="Project Logo">
    
        <h1 class="project_title">
            <a href="/p/vice-emu/" class="project_link">VICE</a>
        </h1>
    
    
    
    <h2 class="project_summary with-icon">
        Versatile Commodore Emulator
    </h2>
    
    <div class="brought-by with-icon">
        Brought to you by:
        
        
            
                <a href="/u/blackystardust/">blackystardust</a>,
            
            
                <a href="/u/gpz/">gpz</a>
            </div>
    

        
    
</div>
    <div id="top_nav" class="">
        
            
<div id="top_nav_admin">
<ul class="dropdown">
  
    <li class="">
        <a href="/projects/vice-emu/" class="tool-summary-32">
            Summary
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/projects/vice-emu/files/" class="tool-files-32">
            Files
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/projects/vice-emu/reviews" class="tool-reviews-32">
            Reviews
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/projects/vice-emu/support" class="tool-support-32">
            Support
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/p/vice-emu/wiki/" class="tool-wiki-32">
            Wiki
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/p/vice-emu/mailman/" class="tool-mailman-32">
            Mailing Lists
        </a>
        
        
    </li>
  
    <li class="selected">
        <a href="/p/vice-emu/code/" class="tool-svn-32">
            Code
        </a>
        
        
    </li>
  
    <li class="">
        <a href="/p/vice-emu/_list/tickets" class="tool-tickets-32">
            Tickets ▾
        </a>
        
        
            <ul>
                
                    <li class=""><a href="/p/vice-emu/bugs/">Bugs</a></li>
                
                    <li class=""><a href="/p/vice-emu/patches/">Patches</a></li>
                
                    <li class=""><a href="/p/vice-emu/feature-requests/">Feature Requests</a></li>
                
            </ul>
        
    </li>
  
  
</ul>
</div>


        
    </div>
    <div id="content_base">
        
            
                
                    


<div id="sidebar">
  
    <div>&nbsp;</div>
  
    
    
      
      
        
    
      <ul class="sidebarmenu">
      
    
  <li>
      
        <a class="icon" href="/p/vice-emu/code/commit_browser" title="Browse Commits"><i class="fa fa-list"></i>
      
      <span>Browse Commits</span>
      </a>
  </li>
  
      
    
    
      </ul>
      
    
    
</div>
                
                
            
            
                
            
            <div class="grid-20 pad">
                <h2 class="dark title">
<a href="/p/vice-emu/code/31403/">[r31403]</a>:

  
  
    <a href="./../../">testprogs</a> /
    
  
    <a href="./../">CIA</a> /
    
  
    <a href="./">dd0dtest</a> /
    
  
 dd0dtest.prg

                    <!-- actions -->
                    <small>
                        

    
    <a class="icon" href="#" id="maximize-content" title="Maximize"><i class="fa fa-expand"></i>&nbsp;Maximize</a>
    <a class="icon" href="#" id="restore-content" title="Restore"><i class="fa fa-compress"></i>&nbsp;Restore</a>
<a class="icon" href="/p/vice-emu/code/31403/log/?path=/testprogs/CIA/dd0dtest/dd0dtest.prg" title="History"><i class="fa fa-calendar"></i>&nbsp;History</a>

                    </small>
                    <!-- /actions -->
                </h2>
                
                <div>
                    
  

                    
  
    <p>dd0dtest.prg is not known to be viewable in your browser.
    Try to <a href="?force=True">display it</a> anyway or
    <a href="?format=raw">download it</a> instead.</p>
  

                </div>
                
                
            </div>
        
    </div>
</section>
  
<footer id="site-footer">
    <div class="wrapper">
        <nav>
            <h5>SourceForge</h5>
            <a href="/about">About</a>
            <a href="/blog/category/sitestatus/">Site Status</a>
            <a href="http://twitter.com/sfnet_ops">@sfnet_ops</a>
            <a id="allura-notice" href="http://allura.apache.org/">
                <p>Powered by</p>
                <p>Apache Allura™</p>
                <img src="https://a.fsdn.com/allura/nf/1468509372/_ew_/theme/sftheme/images/sftheme/logo-black-svg_g.png" />
            </a>
        </nav>
        <nav>
            <h5>Find and Develop Software</h5>
            <a href="/create/">Create a Project</a>
            <a href="/directory/">Software Directory</a>
            <a href="/top">Top Downloaded Projects</a>
        </nav>
        <nav>
            <h5>Community</h5>
            <a href="/blog/">Blog</a>
            <a href="http://twitter.com/sourceforge">@sourceforge</a>
            <a href="https://library.slashdotmedia.com/">Resources</a>
        </nav>
        <nav>
            <h5>Help</h5>
            <a href="http://p.sf.net/sourceforge/docs">Site Documentation</a>
            <a href="/support">Support Request</a>
        </nav>
    </div>
</footer>
<footer id="site-copyright-footer">
    <div class="wrapper">
        <div id="copyright">
            &copy; 2016 Slashdot Media. All Rights Reserved.<br />
        </div>
        <nav>
            <a href="http://slashdotmedia.com/terms-of-use">Terms</a>
            <a href="http://slashdotmedia.com/privacy-statement/">Privacy</a>
            <span id='teconsent'></span>
            <a href="http://slashdotmedia.com/opt-out-choices">Opt Out Choices</a>
            <a href="http://slashdotmedia.com">Advertise</a>
        </nav>
    </div>
</footer>


    
<div id="newsletter-floating" class="goth-form">
    <h2>Get latest updates about Open Source Projects, Conferences and News.</h2>
    <p>Sign up for the SourceForge newsletter:</p>
    
    <form action="/user/newsletters/subscribe" method="post">
    <div class="form">
        <input type="email" name="X7ejLENeSLrkviwhDfLLKvaTnPuM"  placeholder="email@address.com" value="">
    <input type="submit" value="Subscribe" class="bt">
    </div>
    <div class="fielderror"></div>

    <p class="details">
    <span class="fielderror"></span>
    <label>
        <input type="checkbox" required name="X7-7JH82bE3kYTV2HCeNIOx2jqrk" value="true" >I agree to receive quotes, newsletters and other information from sourceforge.net and its partners regarding IT services and products. I understand that I can withdraw my consent at any time. Please refer to our <a href="https://slashdotmedia.com/privacy-statement/">Privacy Policy</a> or <a href="/support">Contact Us</a> for more details
        <input type="hidden" name="X5ePDFNqNIm4kl0qQViFfaL-w5Co" value="true">
    </label>
    </p>

    <input type="hidden" name="source" value="floating">
    <input type="hidden" name="X5O7JBNCKD3QUmlaRXQa4uKnzY9g" value="DE">
    <input type="hidden" name="X--7JBNCKD3QUmlaRXQp_s66wtvk" value="ip_address">
    <input type="hidden" name="X4-PDBs2SGHk_nEuGa-Forup0kXs" value="sitewide">
    <input type="hidden" name="X4-PDBs2SGHk_nEuGa-Forup0kXs" value="research">
    <input type="hidden" name="X4-PDBs2SGHk_nEuGa-Forup0kXs" value="events">
    <input type="hidden" name="X4-PDBs2SGHk_nEuGa-Forup0kXs" value="thirdparty">
    <input type="hidden" name="X4-PDBs2SGHk_nEuGa-Forup0kXs" value="all">
    <input type="hidden" name="X6-bLBO1KGX96T30S_jRz-TC9MRg" value="">
    
  <input type="hidden" name="_visit_cookie" value="57896332c1c78b77fae76d31"/>
    <input id="w-720" name="timestamp" type="hidden" value="1468650815">
    <input id="w-721" name="spinner" type="hidden" value="X6I2mcb7-fQ1L-Tn1OFUM3NvC1Zw">
    <p class="XNWQOIyTHmFkCRkhiLXzmto">
    <label for="X7-XJH9uHTT0YTV2HCeNIOx2jqrk">You seem to have CSS turned off.
        Please don't fill out this field.</label><br>
    <input id="X7-XJH9uHTT0YTV2HCeNIOx2jqrk" name="X7uXJH9uHTV7_nUvEjhHrGrq98Hc" type="text"><br></p>
    <p class="XNWQOIyTHmFkCRkhiLXzmto">
    <label for="X7-XJH9uHTTwYTV2HCeNIOx2jqrk">You seem to have CSS turned off.
        Please don't fill out this field.</label><br>
    <input id="X7-XJH9uHTTwYTV2HCeNIOx2jqrk" name="X7uXJH9uHTF7_nUvEjhHrGrq98Hc" type="text"><br></p>
</form>

    <a id="btn-float-close">No, thanks</a>
</div>


<div id="messages">
    
</div>


    <!-- ew:body_js -->


    <script type="text/javascript" src="https://a.fsdn.com/allura/nf/1468509372/_ew_/_slim/js?href=allura%2Fjs%2Fjquery.notify.js%3Ballura%2Fjs%2Fjquery.tooltipster.js%3Ballura%2Fjs%2Fmodernizr.js%3Ballura%2Fjs%2Fsylvester.js%3Ballura%2Fjs%2Ftwemoji.min.js%3Ballura%2Fjs%2Fpb.transformie.min.js%3Ballura%2Fjs%2Fallura-base.js%3Ballura%2Fjs%2Fadmin_modal.js%3Bjs%2Fjquery.lightbox_me.js%3Btheme%2Fsftheme%2Fjs%2Fsftheme%2Fshared.js%3Ballura%2Fjs%2Fmaximize-content.js"></script>

    
<!-- /ew:body_js -->



    <!-- ew:body_js_tail -->


    
<!-- /ew:body_js_tail -->




<script type="text/javascript">(function() {
  $('#access_urls .btn').click(function(evt){
    evt.preventDefault();
    var parent = $(this).parents('.btn-bar');
    $(parent).find('input').val($(this).attr('data-url'));
    $(parent).find('span').text($(this).attr('title')+' access');
    $(this).parent().children('.btn').removeClass('active');
    $(this).addClass('active');
  });
  $('#access_urls .btn').first().click();

  
  var repo_status = document.getElementById('repo_status');
  // The repo_status div will only be present if repo.status != 'ready'
  if (repo_status) {
    $('.spinner').show()
    var delay = 500;
    function check_status() {
        $.get('/p/vice-emu/code/status', function(data) {
            if (data.status === 'ready') {
                $('.spinner').hide()
                $('#repo_status h2').html('Repo status: ready. <a href=".">Click here to refresh this page.</a>');
            }
            else {
                $('#repo_status h2 span').html(data.status);
                if (delay < 60000){
                    delay = delay * 2;
                }
                window.setTimeout(check_status, delay);
            }
        });
    }
    var status_checker = window.setTimeout(check_status, delay);
    
  }
}());
</script>

<script type="text/javascript">(function() {
  $(window).bind('hashchange', function(e) {
    var hash = window.location.hash.substring(1);
	if ('originalEvent' in e && 'oldURL' in e.originalEvent) {
      $('#' + e.originalEvent.oldURL.split('#')[1]).css('background-color', 'transparent');
	}
    if (hash !== '' && hash.substring(0, 1) === 'l' && !isNaN(hash.substring(1))) {
      $('#' + hash).css('background-color', '#ffff99');
    }
  }).trigger('hashchange');

  var clicks = 0;
  $('.code_block').each(function(index, element) {
    $(element).bind('click', function() {
      // Trick to ignore double and triple clicks
      clicks++;
      if (clicks == 1) {
        setTimeout(function() {
          if (clicks == 1) {
            var hash = window.location.hash.substring(1);
            if (hash !== '' && hash.substring(0, 1) === 'l' && !isNaN(hash.substring(1))) {
              $('#' + hash).css('background-color', 'transparent');
            }
            $(element).css('background-color', '#ffff99');
            window.location.href = '#' + $(element).attr('id');
          };
          clicks = 0;
        }, 500);
      };
    });
  });
}());
</script>


    


    <!-- Google Code for Remarketing tag -->
    <!-- Remarketing tags may not be associated with personally identifiable information or placed on pages related to sensitive categories. For instructions on adding this tag and more information on the above requirements, read the setup guide: google.com/ads/remarketingsetup -->
    <script type="text/javascript">
        /* <![CDATA[ */
        var google_conversion_id = 1002083962;
        var google_conversion_label = "G_uGCOaBlAQQ-qzq3QM";
        var google_custom_params = window.google_tag_params;
        var google_remarketing_only = true;
        /* ]]> */
    </script>
    <script type="text/javascript" src="//www.googleadservices.com/pagead/conversion.js"> </script>
    <script type="text/javascript" src='//consent-st.truste.com/get?name=notice.js&domain=slashdot.org&c=teconsent&text=true'></script>
    <noscript>
      <div style="display:inline;">
        <img height="1" width="1" style="border-style:none;" alt="" src="//googleads.g.doubleclick.net/pagead/viewthroughconversion/1002083962/?value=0&amp;label=G_uGCOaBlAQQ-qzq3QM&amp;guid=ON&amp;script=0"/>
      </div>
    </noscript>

     
      

<script>
    $(document).ready(function () {
        $(".tooltip").tooltipster({
            animation: 'fade',
            delay: 200,
            theme: 'tooltipster-light',
            trigger: 'hover',
            position: 'right',
            iconCloning: false,
            maxWidth: 300
        }).focus(function () {
            $(this).tooltipster('show');
        }).blur(function () {
            $(this).tooltipster('hide');
        });
    });
</script>
</body>
</html>