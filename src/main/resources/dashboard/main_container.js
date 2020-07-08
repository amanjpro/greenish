const e = React.createElement;

function renderMain(page, gid, jid) {
  if (page == 'state') {
    return (
      <div>
        <h3 key="state_header">All data sets</h3>
        <StateContainer endpoint='state'/>
      </div>
    )
  } else if(page == 'group'){
    return(<GroupContainer group={gid}/>)
  } else if(page == 'job'){
    return(<JobContainer group={gid} job={jid}/>)
  } else { // page == 'main'
    return(
      <div>
        <h1 key="greenish_dashboard_header">Greenish dashboard</h1>
        <SummaryContainer/>
        <h3 key="state_header">Detailed missing periods</h3>
        <StateContainer endpoint='missing'/>
      </div>
    )
  }
}

class MainContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      page: 'main',
      gid: null,
      jid: null
    }
  }

  render() {
    return (
      renderMain(this.state.page, this.state.gid, this.state.jid)
    )
  }
}

const domContainer = document.querySelector('#main_container');
ReactDOM.render(e(MainContainer), domContainer);
