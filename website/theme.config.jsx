import { useConfig } from 'nextra-theme-docs'
import { useRouter } from 'next/router'

const Logo = () => (
  <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700 }}>
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M12 2 3 7v10l9 5 9-5V7l-9-5Zm0 2.3 6.5 3.6L12 11.5 5.5 7.9 12 4.3ZM5 9.6l6 3.3v7L5 16.6V9.6Zm14 0v7l-6 3.3v-7l6-3.3Z"
        fill="#EE342F"
      />
    </svg>
    <span>Convex KMP</span>
  </span>
)

export default {
  logo: <Logo />,
  project: {
    link: 'https://github.com/AndroidPoet/convex-kmp',
  },
  docsRepositoryBase: 'https://github.com/AndroidPoet/convex-kmp/tree/master/website',
  color: {
    hue: 2,
    saturation: 85,
  },
  footer: {
    content: (
      <span>
        MIT © {new Date().getFullYear()}{' '}
        <a href="https://github.com/AndroidPoet/convex-kmp" target="_blank" rel="noreferrer">
          Convex KMP
        </a>
        . A Kotlin Multiplatform client for Convex.
      </span>
    ),
  },
  head: function useHead() {
    const { frontMatter } = useConfig()
    const { asPath } = useRouter()
    const pageTitle = frontMatter?.title
    const title = pageTitle ? `${pageTitle} – Convex KMP` : 'Convex KMP'
    const description =
      frontMatter?.description ??
      'Convex KMP — a type-safe, coroutine-first Kotlin Multiplatform client for Convex, ' +
      'running on Android, iOS, JVM Desktop and browser (Wasm).'
    const base = 'https://androidpoet.github.io/convex-kmp'
    const path = asPath === '/' ? '' : asPath.split('?')[0].split('#')[0]
    const canonical = `${base}${path}`
    const ogImage = `${base}/favicon.svg`
    return (
      <>
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>{title}</title>
        <meta name="description" content={description} />
        <link rel="canonical" href={canonical} />
        <link rel="icon" href={`${base}/favicon.svg`} type="image/svg+xml" />
        <meta name="theme-color" content="#EE342F" />
        <meta property="og:type" content="website" />
        <meta property="og:site_name" content="Convex KMP" />
        <meta property="og:url" content={canonical} />
        <meta property="og:title" content={pageTitle ?? 'Convex KMP'} />
        <meta property="og:description" content={description} />
        <meta property="og:image" content={ogImage} />
        <meta name="twitter:card" content="summary_large_image" />
        <meta name="twitter:title" content={pageTitle ?? 'Convex KMP'} />
        <meta name="twitter:description" content={description} />
        <meta name="twitter:image" content={ogImage} />
      </>
    )
  },
  sidebar: {
    defaultMenuCollapseLevel: 1,
  },
  toc: {
    backToTop: true,
  },
  navigation: {
    prev: true,
    next: true,
  },
  darkMode: true,
}
